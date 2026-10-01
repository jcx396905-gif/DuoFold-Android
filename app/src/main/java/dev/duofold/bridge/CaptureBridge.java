package dev.duofold.bridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceControl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Runs only inside the Shizuku shell UserService, never inside the app process. */
public final class CaptureBridge extends IFoldBridge.Stub {
    private static final int NONE=0,IWM_CAPTURE=1,SCREEN_CAPTURE=2,SURFACE_CAPTURE=3,LEGACY_SCREENSHOT=4;
    private final HandlerThread thread=new HandlerThread("DuoCapture");
    private final Handler handler;
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private volatile boolean running;
    private volatile long heartbeat;
    private volatile long frames;
    private volatile String error="";
    private Surface target;
    private SurfaceControl exclude;
    private Object captureArgs;
    private Object windowManager;
    private Object inputManager;
    private IBinder displayToken;
    private Method capture,createCaptureListener,getCaptureBuffer,legacyScreenshot,inject;
    private boolean argsExclude;
    private String iwmCaptureOwner="android.window.ScreenCapture";
    private MotionEvent lastTouch;
    private ICaptureListener listener;
    private int width,height,interval,rotation,backend;

    public CaptureBridge() {thread.start();handler=new Handler(thread.getLooper());}
    public CaptureBridge(Context context) {this();}

    @Override public Bundle probe() {
        Bundle result=new Bundle();result.putInt("uid",Process.myUid());
        result.putInt("sdk",Build.VERSION.SDK_INT);
        try {
            initInput();
            Throwable failure=null;
            try {initIwmCapture();} catch(Throwable e) {failure=append(failure,e);backend=NONE;}
            if(backend==NONE)try {initScreenCapture();} catch(Throwable e) {failure=append(failure,e);backend=NONE;}
            if(backend==NONE)try {initSurfaceCapture();} catch(Throwable e) {failure=append(failure,e);backend=NONE;}
            if(backend==NONE)try {initLegacyScreenshot();} catch(Throwable e) {
                failure=append(failure,e);
                throw new IllegalStateException("No compatible screen capture API: "+describeTree(failure),failure);
            }
            result.putBoolean("captureApi",true);result.putBoolean("inputApi",inject!=null);
            result.putString("backend",backendName());
            result.putString("message","Shell APIs ready: "+backendName());
        } catch(Throwable e) {result.putString("message",describe(e));}
        return result;
    }

    private void initInput() throws Exception {
        try {
            Class<?> im=Class.forName("android.hardware.input.InputManagerGlobal");
            inputManager=im.getMethod("getInstance").invoke(null);
            inject=im.getMethod("injectInputEvent",InputEvent.class,int.class);
        } catch(ClassNotFoundException|NoSuchMethodException e) {
            Class<?> im=Class.forName("android.hardware.input.InputManager");
            inputManager=im.getMethod("getInstance").invoke(null);
            inject=im.getMethod("injectInputEvent",InputEvent.class,int.class);
        }
    }

    private void initIwmCapture() throws Exception {
        IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class)
                .invoke(null,"window");
        windowManager=Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface",IBinder.class)
                .invoke(null,binder);
        Throwable failure=null;
        for(String owner:new String[]{"android.window.ScreenCapture","android.window.ScreenCaptureInternal"}) {
            try {
                Class<?> api=Class.forName(owner);
                Class<?> args=Class.forName(owner+"$CaptureArgs");
                Class<?> listenerType=Class.forName(owner+"$ScreenCaptureListener");
                // Android can evolve captureDisplay between releases; try known signatures.
                Method captureMethod=null;
                for(Method candidate:Class.forName("android.view.IWindowManager").getMethods()) {
                    if(!"captureDisplay".equals(candidate.getName()))continue;
                    Class<?>[] p=candidate.getParameterTypes();
                    if(p.length>=3 && p[0]==int.class && p[1]==args && p[2]==listenerType) {captureMethod=candidate;break;}
                }
                if(captureMethod==null)throw new NoSuchMethodException("IWindowManager.captureDisplay(int,"+args.getSimpleName()+","+listenerType.getSimpleName()+")");
                capture=captureMethod;
                createCaptureListener=resolveListenerFactory(api);
                getCaptureBuffer=Class.forName(owner+"$SynchronousScreenCaptureListener").getMethod("getBuffer");
                iwmCaptureOwner=owner;backend=IWM_CAPTURE;return;
            } catch(Throwable e) {failure=append(failure,e);}
        }
        if(failure instanceof Exception)throw (Exception)failure;
        throw new IllegalStateException("IWindowManager capture unavailable",failure);
    }

    /** Newer builds may require a handler or flags parameter on the listener factory. */
    private static Method resolveListenerFactory(Class<?> api) throws Exception {
        try {return api.getMethod("createSyncCaptureListener");}
        catch(NoSuchMethodException ignored) {}
        for(Method candidate:api.getMethods()) {
            if(!"createSyncCaptureListener".equals(candidate.getName()))continue;
            Class<?>[] p=candidate.getParameterTypes();
            if(p.length==1 && (p[0]==android.os.Handler.class || p[0]==int.class))return candidate;
        }
        throw new NoSuchMethodException("createSyncCaptureListener");
    }

    private Object newSyncListener() throws Exception {
        if(createCaptureListener.getParameterCount()==0)return createCaptureListener.invoke(null);
        Class<?> p=createCaptureListener.getParameterTypes()[0];
        return createCaptureListener.invoke(null,p==int.class?0:null);
    }

    private void initSurfaceCapture() throws Exception {
        Class<?> surfaceControl=Class.forName("android.view.SurfaceControl");
        displayToken=resolveDisplayToken(surfaceControl);
        Class<?> args=Class.forName("android.view.SurfaceControl$DisplayCaptureArgs");
        capture=surfaceControl.getMethod("captureDisplay",args);
        backend=SURFACE_CAPTURE;
    }

    private void initScreenCapture() throws Exception {
        Class<?> surfaceControl=Class.forName("android.view.SurfaceControl");
        displayToken=resolveDisplayToken(surfaceControl);
        Class<?> screenCapture=Class.forName("android.window.ScreenCapture");
        Class<?> args=Class.forName("android.window.ScreenCapture$DisplayCaptureArgs");
        capture=screenCapture.getMethod("captureDisplay",args);
        backend=SCREEN_CAPTURE;
    }

    private void initLegacyScreenshot() throws Exception {
        Class<?> surfaceControl=Class.forName("android.view.SurfaceControl");
        displayToken=resolveDisplayToken(surfaceControl);
        legacyScreenshot=surfaceControl.getMethod("screenshot",IBinder.class,Surface.class,Rect.class,
                int.class,int.class,boolean.class,int.class);
        backend=LEGACY_SCREENSHOT;
    }

    @Override public void start(Surface surface,Bundle options,ICaptureListener callback) {
        heartbeat=SystemClock.uptimeMillis();
        handler.post(()-> {
            release();
            try {
                Bundle probe=probe();
                if(!probe.getBoolean("captureApi")||!probe.getBoolean("inputApi"))
                    throw new IllegalStateException(probe.getString("message"));
                listener=callback;target=surface;
                exclude=(SurfaceControl)options.getParcelable("exclude");
                width=options.getInt("width");height=options.getInt("height");
                rotation=options.getInt("rotation",0);
                interval=1000/Math.max(10,Math.min(60,options.getInt("fps",30)));
                if(target==null||!target.isValid()||exclude==null||!exclude.isValid())
                    throw new IllegalStateException("Effect layer unavailable; refusing recursive capture.");
                buildCaptureArgs(options);
                if(backend!=LEGACY_SCREENSHOT) {
                    try {markExcluded(exclude);}
                    catch(Throwable markFailure) {
                        if(!argsExclude)throw new IllegalStateException("This ROM cannot exclude the effect layer",markFailure);
                    }
                }
                frames=0;error="";running=true;handler.post(frame);
            } catch(Throwable e) {fail(e);}
        });
    }

    private void markExcluded(SurfaceControl layer) throws Exception {
        try(SurfaceControl.Transaction transaction=new SurfaceControl.Transaction()) {
            boolean applied=false;
            for(Method candidate:SurfaceControl.Transaction.class.getMethods()) {
                if(!"setSkipScreenshot".equals(candidate.getName()))continue;
                Class<?>[] p=candidate.getParameterTypes();
                if(p.length>=2 && p[0]==SurfaceControl.class && p[1]==boolean.class) {
                    try {
                        if(p.length==2)candidate.invoke(transaction,layer,true);
                        else {
                            Object[] argv=new Object[p.length];argv[0]=layer;argv[1]=Boolean.TRUE;
                            for(int i=2;i<p.length;i++)argv[i]=p[i]==long.class?0L:null;
                            candidate.invoke(transaction,argv);
                        }
                        applied=true;break;
                    } catch(InvocationTargetException skipped) {}
                }
            }
            if(!applied)throw new NoSuchMethodException("Transaction.setSkipScreenshot");
            transaction.apply();
        }
    }

    private void buildCaptureArgs(Bundle options) throws Exception {
        captureArgs=null;argsExclude=false;
        if(backend==IWM_CAPTURE) {
            Class<?> builderClass=Class.forName(iwmCaptureOwner+"$CaptureArgs$Builder");
            Object builder=builderClass.getConstructor().newInstance();
            float sx=width/(float)Math.max(1,options.getInt("displayWidth",width));
            float sy=height/(float)Math.max(1,options.getInt("displayHeight",height));
            tryOptional(builderClass,"setFrameScale",builder,float.class,float.class,sx,sy);
            try {
                builderClass.getMethod("setExcludeLayers",SurfaceControl[].class)
                        .invoke(builder,(Object)new SurfaceControl[]{exclude});
                argsExclude=true;
            } catch(NoSuchMethodException ignored) {}
            configureContentPolicy(builderClass,builder);
            captureArgs=builderClass.getMethod("build").invoke(builder);
        } else if(backend==SCREEN_CAPTURE||backend==SURFACE_CAPTURE) {
            String owner=backend==SCREEN_CAPTURE?"android.window.ScreenCapture":"android.view.SurfaceControl";
            Class<?> builderClass=Class.forName(owner+"$DisplayCaptureArgs$Builder");
            Object builder=resolveDisplayCaptureBuilder(builderClass,width,height);
            try {
                builderClass.getMethod("setExcludeLayers",SurfaceControl[].class)
                        .invoke(builder,(Object)new SurfaceControl[]{exclude});
                argsExclude=true;
            } catch(NoSuchMethodException ignored) {}
            configureContentPolicy(builderClass,builder);
            captureArgs=builderClass.getMethod("build").invoke(builder);
        }
    }

    /** Some releases take (IBinder, width, height) or setSize; probe for what exists. */
    private Object resolveDisplayCaptureBuilder(Class<?> builderClass,int width,int height) throws Exception {
        try {
            Object builder=builderClass.getConstructor(IBinder.class,int.class,int.class)
                    .newInstance(displayToken,width,height);
            tryOptional(builderClass,"setSize",builder,int.class,int.class,width,height);
            return builder;
        } catch(NoSuchMethodException ignored) {}
        Object builder=builderClass.getConstructor(IBinder.class).newInstance(displayToken);
        tryOptional(builderClass,"setSize",builder,int.class,int.class,width,height);
        return builder;
    }

    private static void tryOptional(Class<?> builderClass,String name,Object target,Class<?>[] types,Object... values) {
        try {builderClass.getMethod(name,types).invoke(target,values);}
        catch(NoSuchMethodException ignored) {}
        catch(Exception ignored) {}
    }
    private static void tryOptional(Class<?> builderClass,String name,Object target,Class<?> type,Object value) {
        tryOptional(builderClass,name,target,new Class<?>[]{type},value);
    }
    private static void tryOptional(Class<?> builderClass,String name,Object target,Class<?> t1,Class<?> t2,Object v1,Object v2) {
        tryOptional(builderClass,name,target,new Class<?>[]{t1,t2},v1,v2);
    }

    /** Content policy APIs differ per release; a failure here must never abort the capture. */
    private static void configureContentPolicy(Class<?> builderClass,Object builder) {
        for(Method candidate:builderClass.getMethods()) {
            if(!candidate.getName().startsWith("set")||candidate.getParameterCount()!=1)continue;
            Class<?> p=candidate.getParameterTypes()[0];
            String name=candidate.getName();
            boolean relevant=(name.equals("setCaptureSecureLayers")&&p==boolean.class)
                    ||(name.equals("setAllowProtected")&&p==boolean.class)
                    ||(name.equals("setSecureContentPolicy")&&p==int.class)
                    ||(name.equals("setProtectedContentPolicy")&&p==int.class)
                    ||(name.equals("setCaptureSecureLayersPolicy")&&p.isEnum())
                    ||(name.equals("setProtectedContentPolicyEnum")&&p.isEnum());
            if(!relevant)continue;
            try {
                if(p==boolean.class)candidate.invoke(builder,Boolean.FALSE);
                else if(p==int.class)candidate.invoke(builder,0);
                else {
                    Object[] constants=p.getEnumConstants();
                    if(constants!=null)candidate.invoke(builder,constants[0]);
                }
            } catch(Throwable ignored) {}
        }
    }

    private final Runnable frame=new Runnable() {
        @Override public void run() {
            if(!running)return;
            if(SystemClock.uptimeMillis()-heartbeat>3000) {
                fail(new IllegalStateException("Client heartbeat expired."));return;
            }
            long started=SystemClock.uptimeMillis();Bitmap bitmap=null;HardwareBuffer buffer=null;
            try {
                if(exclude==null||!exclude.isValid())throw new IllegalStateException("Excluded layer lost.");
                if(backend==LEGACY_SCREENSHOT) {
                    legacyScreenshot.invoke(null,displayToken,target,new Rect(),width,height,false,rotation);
                } else {
                    Object screenshot;
                    if(backend==IWM_CAPTURE) {
                        Object sync=newSyncListener();
                        capture.invoke(windowManager,0,captureArgs,sync);
                        screenshot=getCaptureBuffer.invoke(sync);
                    } else screenshot=capture.invoke(null,captureArgs);
                    if(screenshot==null)throw new IllegalStateException("System denied screen capture or returned no buffer.");
                    Class<?> sc=screenshot.getClass();
                    buffer=(HardwareBuffer)sc.getMethod("getHardwareBuffer").invoke(screenshot);
                    if(buffer==null)throw new IllegalStateException("Empty capture buffer.");
                    boolean secure=false;
                    try {secure=(boolean)sc.getMethod("containsSecureLayers").invoke(screenshot);}
                    catch(NoSuchMethodException legacyCheckMissing) {
                        try {secure=(boolean)sc.getMethod("hasSecureLayers").invoke(screenshot);}
                        catch(Exception ignored) {}
                    }
                    if(secure||(buffer.getUsage()&HardwareBuffer.USAGE_PROTECTED_CONTENT)!=0)
                        throw new IllegalStateException("Protected content: returning to original display.");
                    ColorSpace color=(ColorSpace)sc.getMethod("getColorSpace").invoke(screenshot);
                    bitmap=Bitmap.wrapHardwareBuffer(buffer,color);
                    if(bitmap==null)throw new IllegalStateException("Cannot wrap capture buffer.");
                    if(!running)return;
                    Canvas canvas=target.lockHardwareCanvas();
                    try {
                        canvas.drawColor(0,PorterDuff.Mode.CLEAR);
                        canvas.drawBitmap(bitmap,null,new Rect(0,0,width,height),paint);
                    } finally {target.unlockCanvasAndPost(canvas);}
                }
                frames++;if(listener!=null)listener.onFrame(frames,SystemClock.uptimeMillis());
                // Android 10/11 have no per-layer exclusion API. Freeze the clean frame
                // captured while the app overlay is transparent, then animate that texture.
                if(backend==LEGACY_SCREENSHOT)return;
            } catch(Throwable e) {fail(e);return;}
            finally {if(bitmap!=null)bitmap.recycle();if(buffer!=null)buffer.close();}
            if(running)handler.postDelayed(this,Math.max(1,interval-(SystemClock.uptimeMillis()-started)));
        }
    };

    @Override public synchronized boolean inject(MotionEvent event) {
        if(!running||inject==null||event==null)return false;
        try {
            boolean ok=(boolean)inject.invoke(inputManager,event,0);if(!ok)return false;
            if(lastTouch!=null)lastTouch.recycle();
            lastTouch=(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)
                    ?null:MotionEvent.obtain(event);return true;
        } catch(Throwable e) {error=describe(e);return false;}
    }
    @Override public Bundle status() {
        Bundle b=new Bundle();b.putBoolean("running",running);b.putLong("frames",frames);
        b.putString("error",error);b.putString("backend",backendName());return b;
    }
    @Override public void heartbeat() {heartbeat=SystemClock.uptimeMillis();}
    @Override public void stop() {running=false;cancelTouch();handler.post(this::release);}
    private synchronized void cancelTouch() {
        if(lastTouch!=null) {
            try {lastTouch.setAction(MotionEvent.ACTION_CANCEL);inject.invoke(inputManager,lastTouch,0);}
            catch(Throwable ignored) {} finally {lastTouch.recycle();lastTouch=null;}
        }
    }
    private void fail(Throwable e) {
        error=describe(e);running=false;cancelTouch();
        try {if(listener!=null)listener.onError(error);}catch(Exception ignored){}
        release();
    }
    private void release() {
        running=false;handler.removeCallbacks(frame);cancelTouch();
        if(target!=null){target.release();target=null;}
        if(exclude!=null){exclude.release();exclude=null;}
        captureArgs=null;listener=null;
    }
    @Override public void destroy() {stop();handler.post(()->{thread.quitSafely();System.exit(0);});}
    private String backendName() {
        return switch(backend) {
            case IWM_CAPTURE -> iwmCaptureOwner.endsWith("Internal")
                    ?"IWindowManager / ScreenCaptureInternal":"IWindowManager";
            case SCREEN_CAPTURE -> "ScreenCapture";
            case SURFACE_CAPTURE -> "SurfaceControl";
            case LEGACY_SCREENSHOT -> "SurfaceControl legacy";
            default -> "unavailable";
        };
    }
    private static String describe(Throwable t) {
        while(t instanceof InvocationTargetException&&t.getCause()!=null)t=t.getCause();
        return t.getClass().getSimpleName()+": "+String.valueOf(t.getMessage());
    }
    private static IBinder resolveDisplayToken(Class<?> surfaceControl) throws Exception {
        Class<?> displayControl=null;
        try {displayControl=Class.forName("com.android.server.display.DisplayControl");}
        catch(ClassNotFoundException ignored) {}
        Object token=DisplayTokenResolver.resolve(surfaceControl,displayControl);
        if(!(token instanceof IBinder))throw new IllegalStateException("Physical display token has an unexpected type");
        return (IBinder)token;
    }
    private static Throwable append(Throwable root,Throwable next) {
        if(root==null)return next;root.addSuppressed(next);return root;
    }
    private static String describeTree(Throwable root) {
        StringBuilder out=new StringBuilder(describe(root));
        for(Throwable item:root.getSuppressed())out.append(" | ").append(describe(item));
        return out.toString();
    }
}
