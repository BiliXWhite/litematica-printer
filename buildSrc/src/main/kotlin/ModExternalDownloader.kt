import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 通用文件下载工具
 * 特性：
 * 1. 支持任意 HTTP/HTTPS 链接
 * 2. 自定义输出目录（自动创建）
 * 3. 可选文件名（优先级：用户指定 > 服务器响应头 > 链接提取）
 * 4. 超时控制（连接10秒，读取30秒）
 * 5. 文件完整性校验（非空校验）
 * 6. 友好日志输出
 */
object ExternalModDownloader {
    // 默认超时配置（毫秒）
    private const val CONNECT_TIMEOUT = 10000
    private const val READ_TIMEOUT = 30000

    // 默认 User-Agent（避免部分服务器拒绝）
    private val USER_AGENT = "Gradle/${GradleVersion.current().version}"

    /**
     * 下载文件
     * @param project Gradle 项目实例（用于日志和路径处理）
     * @param downloadUrl 下载链接（必填）
     * @param outputDir 输出目录（必填，自动创建）
     * @param fileName 自定义文件名（可选，为 null 时自动识别）
     * @return 下载后的文件对象，失败返回 null
     */
    fun download(
        project: Project,
        downloadUrl: String,
        outputDir: File,
        fileName: String? = null
    ): File? {
        val trimmedUrl = downloadUrl.trim()
        require(trimmedUrl.isNotBlank()) { "下载链接不能为空！" }
        require(outputDir.isDirectory || outputDir.mkdirs()) { "无法创建输出目录：${outputDir.absolutePath}" }
        println()

        return try {
            // 2. 处理文件名（优先级：用户指定 > 响应头 > 链接提取）
            // val targetFileName = fileName ?: getFileNameFromResponse(connection) ?: extractFileNameFromUrl(trimmedUrl)
            val targetFileName = fileName ?: extractFileNameFromUrl(trimmedUrl)
            ?: throw IOException("无法识别文件名，请手动指定 fileName 参数")
            // 3. 构建目标文件
            val targetFile = outputDir.resolve(targetFileName)
            // 4. 检查文件是否已存在（避免重复下载）
            if (targetFile.exists() && targetFile.length() > 0) {
                project.logger.log(LogLevel.LIFECYCLE, "文件已存在，跳过下载：${targetFile.absolutePath}")
                return targetFile
            }
            project.logger.log(LogLevel.LIFECYCLE, "开始下载：$trimmedUrl")
            project.logger.log(LogLevel.LIFECYCLE, "输出目录：${outputDir.absolutePath}")
            // 1. 建立连接，获取响应信息（用于提取文件名和校验）
            val connection = createConnection(trimmedUrl)
            connection.connect()
            // 5. 执行下载
            project.logger.log(LogLevel.LIFECYCLE, "正在下载：${targetFile.absolutePath}")
            downloadFile(connection, targetFile)
            // 6. 校验文件完整性
            if (!targetFile.exists() || targetFile.length() == 0L) {
                throw IOException("下载的文件为空或损坏")
            }
            project.logger.log(LogLevel.LIFECYCLE, "下载成功：${targetFile.absolutePath}")
            targetFile

        } catch (e: IllegalArgumentException) {
            project.logger.log(LogLevel.ERROR, "下载参数错误：${e.message}")
            null
        } catch (e: IOException) {
            project.logger.log(LogLevel.ERROR, "下载失败：${e.message}", e)
            null
        } catch (e: Exception) {
            project.logger.log(LogLevel.ERROR, "未知错误：${e.message}", e)
            null
        }
    }

    /**
     * 创建 HTTP 连接并配置超时和请求头
     */
    private fun createConnection(urlString: String): HttpURLConnection {
        val url = URI.create(urlString).toURL()
        val connection = url.openConnection() as HttpURLConnection
        // 配置超时
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        // 配置请求头
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "*/*")
        connection.instanceFollowRedirects = true  // 自动跟随重定向
        return connection
    }

    /**
     * 从服务器响应头提取文件名
     * 支持 Content-Disposition 响应头（如：attachment; filename="xxx.jar"）
     */
    private fun getFileNameFromResponse(connection: HttpURLConnection): String? {
        return try {
            val disposition = connection.getHeaderField("Content-Disposition")
            if (disposition.isNullOrBlank()) return null
            // 匹配 filename="xxx" 或 filename=xxx 格式
            val filenamePattern = Regex("filename[\"=]?([^\";]+)")
            val matchResult = filenamePattern.find(disposition)
            matchResult?.groupValues?.get(1)?.trim()?.takeIf { it.contains('.') }
        } catch (e: Exception) {
            null  // 提取失败时返回 null， fallback 到链接提取
        }
    }

    /**
     * 从 URL 提取文件名（处理带参数的链接）
     * 示例：
     * - https://xxx.com/mod.jar → mod.jar
     * - https://xxx.com/download?file=mod-1.0.jar → mod-1.0.jar
     * - https://xxx.com/mod.jar?v=123 → mod.jar
     */
    private fun extractFileNameFromUrl(urlString: String): String? {
        return try {
            // 去掉 ? 和 # 后面的参数
            val cleanUrl = urlString.split('?', '#').first()
            // 提取最后一个 / 后的部分
            val fileName = cleanUrl.substringAfterLast('/')
            // 确保文件名有扩展名（至少3个字符，如 .jar、.zip）
            if (fileName.contains('.') && fileName.substringAfterLast('.').length >= 2) {
                fileName
            } else {
                // 无有效扩展名时，默认用 .jar（针对模组场景）
                "downloaded-file-${System.currentTimeMillis()}.jar"
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 实际写入文件
     */
    private fun downloadFile(connection: HttpURLConnection, targetFile: File) {
        connection.inputStream.use { inputStream ->
            Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

/**
 * Gradle 项目扩展函数（简化调用）
 * 示例：project.downloadFile("url", file("outputDir"), "custom.jar")
 */
fun Project.downloadFile(
    downloadUrl: String,
    outputDir: File,
    fileName: String? = null
): File? {
    return ExternalModDownloader.download(this, downloadUrl, outputDir, fileName)
}

/**
 * 重载扩展函数（支持字符串格式的输出目录路径）
 * 示例：project.downloadFile("url", "outputDir", "custom.jar")
 */
fun Project.downloadFile(
    downloadUrl: String,
    outputDirPath: String,
    fileName: String? = null
): File? {
    return downloadFile(downloadUrl, file(outputDirPath), fileName)
}
