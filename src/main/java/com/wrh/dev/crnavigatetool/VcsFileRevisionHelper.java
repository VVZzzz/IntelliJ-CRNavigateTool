package com.wrh.dev.crnavigatetool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用于获取 VirtualFile 特定 revision 版本的工具类
 */
public class VcsFileRevisionHelper {

    /**
     * 同步获取文件的特定版本（适用于已知历史记录已缓存的场景）
     *
     * @param project      当前项目
     * @param file         目标文件（VirtualFile）
     * @param revisionId   版本标识（如 Git 的 commit hash、SVN 的版本号）
     * @return 特定版本的文件内容，若失败则返回 null
     */
    @Nullable
    public static FileRevisionResultWrapper getFileContentByRevision(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull String revisionId
    ) {
        try {
            // 1. 获取 VCS 管理器
            ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
            if (vcsManager == null) {
                return null;
            }

            // 2. 确定文件所属的 VCS（如 Git、SVN）
            AbstractVcs vcs = vcsManager.getVcsFor(file);
            if (vcs == null) {
                // 文件不受版本控制
                return null;
            }

            // 3. 获取历史记录提供者
            VcsHistoryProvider historyProvider = vcs.getVcsHistoryProvider();
            if (historyProvider == null) {
                return null;
            }

            // 4. 转换为 VCS 识别的文件路径
            FilePath filePath = new LocalFilePath(file.getPath(), file.isDirectory());
            //FilePath filePath = ProjectLevelVcsManager.getFilePath(file);

            // 5. 获取文件的版本历史（同步方式，可能耗时）
            VcsHistorySession historySession = historyProvider.createSessionFor(filePath);
            List<VcsFileRevision> revisions = historySession.getRevisionList();

            // 6. 查找目标 revision(revisions 按照时间倒序)
            // 如果查不到对应 revisionId 有两个情况,通过时间判断确认:
            // 1.该文件在该 revisionId 版本之前是有的, revisionId 版本它没有对应的变更
            // 2.该文件在该 revisionId 版本之前从未被添加过
            boolean notExistBeforeTargetRevision = false;
            for (VcsFileRevision revision : revisions) {
                // todo: 需要传入原文件的 vituralfile  来找到对应 revisionId 对应的时间 date

                // 不同 VCS 的 revision 标识格式不同，通过 asString() 统一比较
                if (revisionId.equals(revision.getRevisionNumber().asString())) {
                    // 7. 读取该版本的文件内容（字节数组转字符串）
                    return new FileRevisionResultWrapper(revision, FileRevisionNoFoundReason.Unknown);

                }
            }




        } catch (VcsException e) {
            // 处理 VCS 操作异常（如历史记录获取失败）
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 异步获取文件的特定版本（适用于需要避免 UI 阻塞的场景）
     *
     * @param project      当前项目
     * @param file         目标文件
     * @param revisionId   版本标识
     * @param callback     回调函数（处理结果）
     */
    /*
    public static void getFileContentByRevisionAsync(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull String revisionId,
            @NotNull Consumer<@Nullable String> callback
    ) {
        // 异步执行，避免阻塞 UI 线程
        new Thread(() -> {
            String content = getFileContentByRevision(project, file, revisionId);
            // 切换回 UI 线程执行回调（若需要更新 UI）
            project.getMessageBus().syncPublisher(VcsManager.TOPIC).onRefresh();
            callback.consume(content);
        }).start();
    }
     */
}
