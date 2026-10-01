package dev.duofold.bridge;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import rikka.shizuku.Shizuku;

import static dev.duofold.UiText.t;

public final class BridgeClient {
    private static BridgeClient instance;
    /** Bump whenever CaptureBridge behaviour changes so Shizuku restarts the cached user service. */
    private static final int USER_SERVICE_VERSION=2;
    private static final long BIND_TIMEOUT_MS=15000;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final CopyOnWriteArrayList<Runnable> observers=new CopyOnWriteArrayList<>();
    private final Shizuku.UserServiceArgs args;
    private volatile IFoldBridge bridge;
    private volatile String message=t("等待 Shizuku","Waiting for Shizuku");
    private boolean binding;
    private int bindAttempt;
    private volatile int probeGeneration;
    private final AtomicBoolean timeoutArmed=new AtomicBoolean();
    private BridgeClient(Context context) {
        args=new Shizuku.UserServiceArgs(new ComponentName(context,CaptureBridge.class))
                .daemon(false).processNameSuffix("capture").debuggable(true).version(USER_SERVICE_VERSION);
        Shizuku.addBinderReceivedListenerSticky(()->main.post(this::connect));
        Shizuku.addBinderDeadListener(()->main.post(()-> {
            bridge=null;binding=false;bindAttempt=0;message=t("Shizuku 已断开","Shizuku disconnected");notifyObservers();
        }));
        Shizuku.addRequestPermissionResultListener((code,result)-> {
            if(result==PackageManager.PERMISSION_GRANTED) connect();
            else { message=t("未获得 Shizuku 授权","Shizuku permission denied"); notifyObservers(); }
        });
    }
    public static synchronized BridgeClient get(Context context) {
        if(instance==null) instance=new BridgeClient(context.getApplicationContext()); return instance;
    }
    public IFoldBridge service() { return bridge; }
    public String message() { return message; }
    public void addObserver(Runnable observer) { observers.add(observer); }
    public void removeObserver(Runnable observer) { observers.remove(observer); }
    public void request() {
        try {
            if(!Shizuku.pingBinder()) {message=t("请先启动 Shizuku","Start Shizuku first");notifyObservers();return;}
            if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(41);
            else connect();
        } catch(Exception e) {message=e.toString();notifyObservers();}
    }
    public void connect() {
        try {
            if(bridge!=null || binding || !Shizuku.pingBinder()) return;
            if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED) {
                message=t("Shizuku 已启动，等待授权","Shizuku is running; waiting for authorization");notifyObservers();return;
            }
            binding=true; message=t("正在连接系统画面服务","Connecting to the screen capture service");notifyObservers();
            try {Shizuku.unbindUserService(args,connection,true);} catch(Throwable ignored) {}
            Shizuku.bindUserService(args,connection);
            armBindingTimeout();
        } catch(Exception e) {binding=false;message=e.toString();notifyObservers();}
    }
    /** If Shizuku never starts the user service (wireless adb dropped, ROM killed it, stale cached
     *  process), reset the state and retry once before surfacing actionable guidance. */
    private void armBindingTimeout() {
        timeoutArmed.set(true);
        main.postDelayed(()-> {
            if(!timeoutArmed.compareAndSet(true,false)) return;
            if(bridge!=null || !binding) return;
            binding=false;
            try {Shizuku.unbindUserService(args,connection,true);} catch(Throwable ignored) {}
            bindAttempt++;
            if(bindAttempt<=2) {message=t("连接超时，正在重试 (n/2)","Connection timed out; retrying (n/2)").replace("n",String.valueOf(bindAttempt));notifyObservers();connect();}
            else {
                message=t("连接系统画面服务超时：请确认 Shizuku 正在运行（无线调试需保持连接），在 Shizuku 应用内停止本应用的用户服务后重试",
                        "Screen capture service connection timed out: make sure Shizuku is running (keep wireless debugging connected), stop Duo Fold's user service inside the Shizuku app, then retry");
                notifyObservers();
            }
        },BIND_TIMEOUT_MS);
    }
    private void onBound(IFoldBridge candidate,String readyMessage) {
        timeoutArmed.set(false);binding=false;bindAttempt=0;bridge=candidate;message=readyMessage;notifyObservers();
    }
    private void onBindFailed(String reason) {
        timeoutArmed.set(false);binding=false;bridge=null;message=reason;notifyObservers();
    }
    private final ServiceConnection connection=new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name,IBinder binder) {
            IFoldBridge candidate=IFoldBridge.Stub.asInterface(binder);
            final int session=probeGeneration;
            worker.execute(()-> {
                try {
                    Bundle probe=candidate.probe();
                    if(session!=probeGeneration) return; // a newer bind superseded this probe
                    boolean ready=probe.getBoolean("captureApi") && probe.getBoolean("inputApi");
                    String text=ready?t("Shizuku 就绪 · UID ","Shizuku ready · UID ")+probe.getInt("uid")+" · "+probe.getString("backend")
                            :probe.getString("message");
                    main.post(()-> {if(session!=probeGeneration)return;onBound(ready?candidate:null,text);});
                } catch(Exception e) {
                    main.post(()->{if(session==probeGeneration)onBindFailed(e.toString());});
                }
            });
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            probeGeneration++;bridge=null;binding=false;
            message=t("系统画面服务已断开","Screen capture service disconnected");notifyObservers();
            main.postDelayed(()-> {if(bridge==null && !binding) connect();},1000);
        }
        @Override public void onBindingDied(ComponentName name) {
            probeGeneration++;onBindFailed(t("系统画面服务绑定失效，正在重连","Capture service binding died; reconnecting"));
            bindAttempt=0;connect();
        }
        @Override public void onNullBinding(ComponentName name) {
            probeGeneration++;bindAttempt=0;
            onBindFailed(t("Shizuku 无法启动系统画面服务：请更新 Shizuku，或在其应用内停止 Duo Fold 的用户服务后重新连接",
                    "Shizuku failed to start the capture service: update Shizuku, or stop Duo Fold's user service inside the Shizuku app, then reconnect"));
        }
    };
    private void notifyObservers() { main.post(()->observers.forEach(Runnable::run)); }
}
