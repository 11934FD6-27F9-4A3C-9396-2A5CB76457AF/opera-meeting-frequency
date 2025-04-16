package meeting;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import meeting.frequency.Handler;

import java.util.logging.Logger;

public class Function {

    @FunctionName("weekly-report-trigger")
    public void weeklyRepost(
            final @TimerTrigger(
                    name = "dailyAlertTrigger",
                    schedule = "0 */5 * * * *"
            ) String timerInfo,
            ExecutionContext context) {
        Logger logger = context.getLogger();
        final Handler handler = new Handler();

        handler.weeklyRepost(logger);
    }

}
