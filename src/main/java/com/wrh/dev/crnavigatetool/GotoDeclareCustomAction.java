package com.wrh.dev.crnavigatetool;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsCollectorImpl;
import com.intellij.internal.statistic.eventLog.events.EventFields;
import com.intellij.internal.statistic.eventLog.events.EventPair;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GotoDeclareCustomAction extends GotoDeclarationAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("enter Custom GotoDeclareCustomAction");
        super.actionPerformed(e);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        System.out.println("CustomAction update called");
        super.update(e);
    }

}
