package com.wrh.dev.crnavigatetool;

/*
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.APP)
public final class DiffHandlerOverride implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        // 确保只初始化一次
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            overrideHandlers();
        }
    }

    private void overrideHandlers() {
        overrideHandler("GotoDeclaration");
        overrideHandler("GotoImplementation");
        overrideHandler("GotoTypeDeclaration");
        overrideHandler("FindUsages");
    }

    private void overrideHandler(String actionId) {
        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        AnAction action = actionManager.getAction(actionId);

        if (action instanceof EditorAction) {
            EditorAction editorAction = (EditorAction) action;
            EditorActionHandler originalHandler = editorAction.getHandler();
            EditorActionHandler newHandler = new CRNavigationHandler(originalHandler);
            editorAction.setupHandler(newHandler);
        }
    }

    public static void initialize() {
        // 确保服务被加载
        ApplicationManager.getApplication().getService(DiffHandlerOverride.class);
    }
}

 */