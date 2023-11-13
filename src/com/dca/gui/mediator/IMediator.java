package com.dca.gui.mediator;

import com.dca.gui.components.MainController;
import com.dca.gui.TabSpace;
import com.dca.gui.components.FindReplaceToolBar;
import com.dca.gui.components.MainMenuBar;

import java.nio.file.Path;
import java.util.List;

public interface IMediator {

    void setMenuBar(MainMenuBar mainMenuBar);
    void setTabSpaces(List<TabSpace> tabSpaces);
    void setMainController(MainController mainController);
    void setFindReplaceToolBar(FindReplaceToolBar findReplaceToolBar);

    String getText();
    Path getFilePath();
    boolean isFileSaved();
    boolean shouldExit();
    boolean isMatchCase();
    Mediator.EventBuilder getEventBuilder();

    String getMediatorText();
    Path getMediatorFilePath();

}
