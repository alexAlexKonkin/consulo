package consulo.web.application.impl;

import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Splash;
import consulo.annotations.RequiredDispatchThread;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends ApplicationImpl implements WebApplication {
  private WebSession myCurrentSession;

  public WebApplicationImpl(boolean isInternal, boolean isUnitTestMode, boolean isHeadless, boolean isCommandLine, @NotNull String appName, @Nullable Splash splash) {
    super(isInternal, isUnitTestMode, isHeadless, isCommandLine, appName, splash);
  }

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@NotNull Runnable process,
                                                     @NotNull String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project,
                                                     JComponent parentComponent,
                                                     String cancelText) {
    ProgressManager.getInstance().runProcess(process, new EmptyProgressIndicator());
    return true;
  }

  @Override
  public void setCurrentSession(@Nullable WebSession session) {
    myCurrentSession = session;
  }

  @Override
  @Nullable
  public WebSession getCurrentSession() {
    return myCurrentSession;
  }
}
