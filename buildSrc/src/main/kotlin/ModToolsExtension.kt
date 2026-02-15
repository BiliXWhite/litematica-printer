import org.gradle.api.Project
import org.gradle.api.initialization.Settings

object BuildToolUtils {
    fun parseMcVersionToNumber(mcVersionStr: String): Int {
        // 空值/空白处理
        if (mcVersionStr.isBlank()) return 0

        try {
            // 步骤1：移除后缀（-fabric/-pre/-rc/-snapshot 等）
            val cleanVersion = mcVersionStr.split("-")[0]
                // 步骤2：仅保留数字和点（过滤非版本字符）
                .replace(Regex("[^0-9.]"), "")

            // 步骤3：分割版本段并转换为数字
            val versionParts = cleanVersion.split(".")
                .filter { it.isNotEmpty() } // 过滤空段（避免异常分割）

            val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = versionParts.getOrNull(2)?.toIntOrNull() ?: 0

            // 组合为 5 位数字（如 1.21.11 → 1*10000 + 21*100 + 11 = 12111）
            return major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            // 异常版本号（如 "invalid"）返回 0，避免构建中断
            println("解析 Minecraft 版本失败：$mcVersionStr，异常：${e.message}")
            return 0
        }
    }

    /**
     * 反向：将数字版本转为字符串（如 12111 → "1.21.11"，12006 → "1.20.6"）
     */
    fun formatMcVersionNumber(mcVersionInt: Int): String {
        if (mcVersionInt <= 0) return "unknown"
        val major = mcVersionInt / 10000
        val minor = (mcVersionInt % 10000) / 100
        val patch = mcVersionInt % 100
        return if (patch > 0) "$major.$minor.$patch" else "$major.$minor"
    }

    // ========== 可扩展其他全局工具函数 ==========
    /**
     * 示例：清理字符串中的特殊字符（用于文件名/模组ID）
     */
    fun cleanSpecialChars(str: String): String {
        return str.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}

fun parseMcVersionToNumber(mcVersionStr: String): Int = BuildToolUtils.parseMcVersionToNumber(mcVersionStr)
fun formatMcVersionNumber(mcVersionInt: Int): String = BuildToolUtils.formatMcVersionNumber(mcVersionInt)
fun cleanSpecialChars(str: String): String = BuildToolUtils.cleanSpecialChars(str)

fun Settings.parseMcVersionToNumber(mcVersionStr: String): Int = BuildToolUtils.parseMcVersionToNumber(mcVersionStr)
fun Settings.formatMcVersionNumber(mcVersionInt: Int): String = BuildToolUtils.formatMcVersionNumber(mcVersionInt)

fun Project.parseMcVersionToNumber(mcVersionStr: String): Int = BuildToolUtils.parseMcVersionToNumber(mcVersionStr)
fun Project.formatMcVersionNumber(mcVersionInt: Int): String = BuildToolUtils.formatMcVersionNumber(mcVersionInt)