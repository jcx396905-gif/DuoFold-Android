package dev.duofold.motion;

import java.util.Locale;

/** Persist stable IDs rather than UI positions so upgrades keep the selected effect. */
public enum EffectStyle {
    CLASSIC("classic", 0, "经典磨砂 · 默认", "Classic Frosted · default",
            "保留当前效果。倾斜越大、远离固定边缘越远，模糊和暗部越明显；停住时保持磨砂。",
            "Keeps the current look. The more you tilt and the further from the fixed edge, the stronger the blur and shading; frosted while holding still."),
    EDGE("edge", 1, "边缘渐隐", "Edge Fade",
            "固定边缘保持清楚，模糊与暗部逐渐集中到展开的一侧；停住时保持渐变。",
            "The fixed edge stays sharp while blur and shading gather on the opening side; the gradient holds while still."),
    SOFT("soft", 2, "柔和景深", "Soft Depth",
            "使用更柔和的模糊与较轻的暗部，停住时保留轻盈的玻璃景深。",
            "Softer blur and lighter shading, keeping a gentle glass depth while still."),
    SETTLE("settle", 3, "悬停清晰", "Hover Clear",
            "移动时出现模糊，停住约半秒后逐渐清晰；透视位置保持不动，再次移动恢复模糊。",
            "Blurs while moving, sharpens about half a second after you stop; the perspective stays put and blurring returns on the next move."),
    CLEAR("clear", 4, "清透投影", "Clear Projection",
            "保留翻折透视，关闭磨砂模糊与额外暗部，移动和停住时都保持清晰。",
            "Keeps the folding perspective without frosted blur or extra shading; stays clear while moving and still."),
    BOKEH("bokeh", 5, "镜头散景", "Lens Bokeh",
            "独立镜头透视，近边清晰、远边逐渐化开；停住时保持景深。空间强度控制光圈，100% 为完整散景。",
            "Independent lens perspective: near edge sharp, far edge melting away; depth holds while still. Spatial strength is the aperture, 100% for full bokeh.");

    public final String id, zhTitle, enTitle, zhDescription, enDescription;
    public final int shaderId;
    EffectStyle(String id,int shaderId,String zhTitle,String enTitle,String zhDescription,String enDescription) {
        this.id=id;this.shaderId=shaderId;
        this.zhTitle=zhTitle;this.enTitle=enTitle;
        this.zhDescription=zhDescription;this.enDescription=enDescription;
    }
    private static final boolean ENGLISH;
    static { ENGLISH=!"zh".equals(Locale.getDefault().getLanguage()); }
    public String label() { return ENGLISH?enTitle:zhTitle; }
    public String summary() { return ENGLISH?enDescription:zhDescription; }
    /** @deprecated use {@link #label()} — kept so older call sites still compile. */
    @Deprecated public String title() { return label(); }
    public static EffectStyle fromId(String id) {
        for(EffectStyle style:values())if(style.id.equals(id))return style;
        return CLASSIC;
    }
}
