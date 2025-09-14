package com.wrh.dev.crnavigatetool;

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 将 VcsFileRevision 转换为 VirtualFile 的工具类
 */
public class VcsRevisionToVirtualFile {

    /**
     * 将 VcsFileRevision 转换为 VirtualFile（通过临时文件）
     *
     * @param revision 历史版本对象
     * @param fileName 文件名字
     * @return 临时文件对应的 VirtualFile，失败返回 null
     */
    @Nullable
    public static VirtualFile convert(@NotNull VcsFileRevision revision, String fileName) {
        File tempFile = null;
        try {
            // 1. 获取历史版本的文件内容
            byte[] content = revision.loadContent();
            if (content == null) {
                return null; // 内容为空，无法创建文件
            }

            // 2. 创建临时文件（使用原文件名避免冲突）
            tempFile = Files.createTempFile("vcs-revision-", "-" + fileName).toFile();

            // 3. 写入历史版本内容到临时文件
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(content);
            }

            // 4. 将临时文件转换为 VirtualFile
            return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile);

        } catch (IOException | VcsException e) {
            e.printStackTrace();
            return null;
        } finally {
            // 注意：如果需要长期使用 VirtualFile，不要在这里删除临时文件
            // 可以在使用完毕后（如关闭编辑器时）手动删除
            if (tempFile != null) tempFile.deleteOnExit(); // 程序退出时自动清理
        }
    }
}
