package com.example.nf.web;

import com.example.nf.common.ApiResponse;
import com.example.nf.dto.*;
import com.example.nf.service.NioFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NIO.2 文件操作 REST 接口。
 *
 * <p>按照微信公众号原文结构组织接口：Path 路径运算、Files 创建、读写、复制移动、遍历、属性。</p>
 * <p>所有接口统一返回 {@link ApiResponse}，data 为业务结果。</p>
 */
@RestController
@RequestMapping("/api/nio")
@RequiredArgsConstructor
@Tag(name = "NIO.2 文件操作", description = "Path + Files 实战接口")
public class NioFileController {

    private final NioFileService nioFileService;

    // ==================== Path 四兄弟 ====================

    @GetMapping("/path/resolve")
    @Operation(summary = "Path.resolve：拼接子路径")
    public ApiResponse<Map<String, String>> resolve(
            @RequestParam(required = false) String base,
            @RequestParam String other) {
        return ApiResponse.ok(nioFileService.resolve(base, other));
    }

    @GetMapping("/path/resolveSibling")
    @Operation(summary = "Path.resolveSibling：替换兄弟节点")
    public ApiResponse<Map<String, String>> resolveSibling(
            @RequestParam(required = false) String path,
            @RequestParam String sibling) {
        return ApiResponse.ok(nioFileService.resolveSibling(path, sibling));
    }

    @GetMapping("/path/relativize")
    @Operation(summary = "Path.relativize：求相对路径")
    public ApiResponse<Map<String, String>> relativize(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ApiResponse.ok(nioFileService.relativize(from, to));
    }

    @GetMapping("/path/normalize")
    @Operation(summary = "Path.normalize：规范化路径")
    public ApiResponse<Map<String, String>> normalize(
            @RequestParam(required = false) String path) {
        return ApiResponse.ok(nioFileService.normalize(path));
    }

    @GetMapping("/path/to-and-from-file")
    @Operation(summary = "Path 与 File 互转")
    public ApiResponse<Map<String, String>> toAndFromFile(
            @RequestParam(required = false) String path) {
        return ApiResponse.ok(nioFileService.toAndFromFile(path));
    }

    // ==================== 创建与删除 ====================

    @PostMapping("/file/create")
    @Operation(summary = "创建文件/目录/临时文件/临时目录")
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateRequest request) throws IOException {
        return ApiResponse.ok(nioFileService.create(
                request.getPath(),
                request.getType(),
                request.getPrefix(),
                request.getSuffix()));
    }

    @PostMapping("/file/delete")
    @Operation(summary = "删除文件或目录")
    public ApiResponse<Map<String, Object>> delete(@RequestBody DeleteRequest request) throws IOException {
        return ApiResponse.ok(nioFileService.delete(request.getPath(), request.isRecursive()));
    }

    // ==================== 读写 ====================

    @PostMapping("/file/write")
    @Operation(summary = "覆盖写入文本")
    public ApiResponse<Map<String, Object>> write(@RequestBody WriteRequest request) throws IOException {
        return ApiResponse.ok(nioFileService.writeText(request.getPath(), request.getContent()));
    }

    @GetMapping("/file/read")
    @Operation(summary = "读取全部文本")
    public ApiResponse<Map<String, Object>> read(@RequestParam String path) throws IOException {
        return ApiResponse.ok(nioFileService.readText(path));
    }

    @GetMapping("/file/lines")
    @Operation(summary = "按行读取")
    public ApiResponse<Map<String, Object>> lines(@RequestParam String path) throws IOException {
        return ApiResponse.ok(nioFileService.readLines(path));
    }

    @GetMapping("/file/lineStats")
    @Operation(summary = "流式统计行信息")
    public ApiResponse<Map<String, Object>> lineStats(@RequestParam String path) throws IOException {
        return ApiResponse.ok(nioFileService.lineStats(path));
    }

    @PostMapping("/file/writeLines")
    @Operation(summary = "使用 BufferedWriter 写入多行")
    public ApiResponse<Map<String, Object>> writeLines(@RequestBody LinesRequest request) throws IOException {
        return ApiResponse.ok(nioFileService.writeWithBufferedWriter(request.getPath(), request.getLines()));
    }

    @GetMapping("/file/readLinesBuffered")
    @Operation(summary = "使用 BufferedReader 读取多行")
    public ApiResponse<Map<String, Object>> readLinesBuffered(@RequestParam String path) throws IOException {
        return ApiResponse.ok(nioFileService.readWithBufferedReader(path));
    }

    @PostMapping("/file/writeProperties")
    @Operation(summary = "写入 Properties 文件")
    public ApiResponse<Map<String, Object>> writeProperties(@RequestBody PropertiesWriteRequest request) throws IOException {
        return ApiResponse.ok(nioFileService.writeProperties(request.getPath(), request.getProperties()));
    }

    @GetMapping("/file/readProperties")
    @Operation(summary = "读取 Properties 文件")
    public ApiResponse<Map<String, Object>> readProperties(@RequestParam String path) throws IOException {
        return ApiResponse.ok(nioFileService.readProperties(path));
    }

    // ==================== 复制与移动 ====================

    @PostMapping("/file/copy")
    @Operation(summary = "复制文件")
    public ApiResponse<Map<String, Object>> copy(@RequestBody CopyRequest request) throws IOException {
        return ApiResponse.ok(nioFileService.copy(
                request.getSrc(),
                request.getDst(),
                request.isReplaceExisting(),
                request.isCopyAttributes()));
    }

    @PostMapping("/file/copyDirectory")
    @Operation(summary = "递归复制目录")
    public ApiResponse<Map<String, Object>> copyDirectory(@RequestBody CopyDirectoryRequest request) throws IOException {
        return ApiResponse.ok(nioFileService.copyDirectory(request.getSrc(), request.getDst()));
    }

    @PostMapping("/file/move")
    @Operation(summary = "移动/重命名文件")
    public ApiResponse<Map<String, Object>> move(@RequestBody MoveRequest request) throws IOException {
        return ApiResponse.ok(nioFileService.move(request.getSrc(), request.getDst(), request.isAtomic()));
    }

    @PostMapping("/file/upload")
    @Operation(summary = "文件上传到沙箱（Files.copy 从输入流落地）")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String dst) throws IOException {
        return ApiResponse.ok(nioFileService.upload(file.getInputStream(), dst));
    }

    // ==================== 遍历 ====================

    @GetMapping("/file/list")
    @Operation(summary = "列出当前目录下一层")
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String dir) throws IOException {
        return ApiResponse.ok(nioFileService.list(dir));
    }

    @GetMapping("/file/walk")
    @Operation(summary = "递归遍历目录文件")
    public ApiResponse<Map<String, Object>> walk(
            @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "0") int maxDepth) throws IOException {
        return ApiResponse.ok(nioFileService.walk(dir, maxDepth));
    }

    @GetMapping("/file/stats")
    @Operation(summary = "walkFileTree 统计目录")
    public ApiResponse<Map<String, Object>> stats(@RequestParam(required = false) String dir) throws IOException {
        return ApiResponse.ok(nioFileService.walkFileTreeStats(dir));
    }

    // ==================== 属性 ====================

    @GetMapping("/file/properties")
    @Operation(summary = "查看文件/目录属性")
    public ApiResponse<Map<String, Object>> properties(@RequestParam String path) throws IOException {
        return ApiResponse.ok(nioFileService.properties(path));
    }

    // ==================== 八股速记 ====================

    @GetMapping("/explain")
    @Operation(summary = "八股速记：Path 与 Files 核心考点")
    public ApiResponse<Map<String, Object>> explain() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("title", "Java NIO.2 Path + Files 核心八股");

        Map<String, String> path = new LinkedHashMap<>();
        path.put("resolve", "把子路径拼到基路径；参数为绝对路径则直接返回参数");
        path.put("resolveSibling", "替换同一目录下的兄弟文件名，常用于改名");
        path.put("relativize", "求从 A 到 B 的相对路径；要求同绝对或同相对");
        path.put("normalize", "纯字符串清理 . 和 ..，不访问磁盘");
        path.put("toFile / toPath", "Path 与 File 只是视图互转，底层同一路径");
        result.put("path", path);

        Map<String, String> files = new LinkedHashMap<>();
        files.put("createFile / createDirectory / createTempFile / createTempDirectory", "创建各类文件与临时文件");
        files.put("delete / deleteIfExists", "删除文件或空目录");
        files.put("readAllBytes / readAllLines", "小文件一次性读取");
        files.put("newBufferedReader / newBufferedWriter", "大文件流式读写，效率更高");
        files.put("newInputStream / newOutputStream", "与字节流、Properties 等老 API 桥接");
        files.put("copy", "复制文件；复制目录只复制空目录本身");
        files.put("move", "移动或重命名；ATOMIC_MOVE 同文件系统内原子");
        files.put("list / walk / walkFileTree", "目录遍历；list 只一层，walk 递归，walkFileTree 可自定义访问器");
        files.put("exists / isDirectory / isRegularFile / size / getLastModifiedTime", "常用属性判断");
        result.put("files", files);

        Map<String, String> tips = new LinkedHashMap<>();
        tips.put("Stream 关闭", "Files.list / walk / lines 都返回 Stream，必须用 try-with-resources 关闭，否则句柄泄漏");
        tips.put("原子移动", "ATOMIC_MOVE 不能跨文件系统，跨盘会抛 AtomicMoveNotSupportedException");
        tips.put("目录复制", "Files.copy 不递归，完整复制需要 walk + relativize + resolve 自行实现");
        tips.put("沙箱安全", "生产环境应把用户输入路径限制在业务目录内，normalize 后校验前缀");
        result.put("tips", tips);

        return ApiResponse.ok(result);
    }
}
