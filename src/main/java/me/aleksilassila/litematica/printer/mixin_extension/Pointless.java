package me.aleksilassila.litematica.printer.mixin_extension;

/**
 * 多版本适配专用的空占位类
 * <p>
 * 用途：在Minecraft不同版本适配时，当当前版本不存在目标Mixin类（如jackf.ChestTrackerScreenMixin）时，
 * 用此类替代原Mixin类的引用，避免因Mixin类缺失导致的InvalidMixinException异常，
 * 本质是让当前版本跳过该Mixin的处理逻辑（即不对当前版本做任何Mixin修改）。
 * <p>
 * 设计背景：不同MC版本的类结构/包路径可能不同，部分Mixin类仅适配特定版本，
 * 对于不支持的版本，通过此类占位可保证模组在多版本下正常启动，不会因Mixin类缺失崩溃。
 */
public class Pointless {
}