package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.matsim.analysis.KelheimMainModeIdentifier;
import org.matsim.analysis.ModeChoiceCoverageControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.automatedCalibration.AutomaticScenarioCalibrator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.run.utils.StrategyWeightFadeout;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandLine.Command(
        name = "auto-tuning",
        description = "Auto tune the Kelheim scenario"
)
public class RunKelheimAutoTuning implements MATSimAppCommand {
    @CommandLine.Option(names = "--config", description = "config file", required = true)
    private String configFile;

    @CommandLine.Option(names = "--output", description = "output folder", defaultValue = "")
    private String outputFolder;

    @CommandLine.Option(names = "--reference", description = "Path to reference data", required = true)
    private String referenceDataFile;

    @CommandLine.Option(names = "--target", description = "target error", defaultValue = "0.001")
    private double targetError;

    @CommandLine.Option(names = "--time", description = "target error", defaultValue = "604800")
    private long maxRunningTime;

    @CommandLine.Option(names = "--patience", description = "patience for the tuning", defaultValue = "5")
    private int patience;

    @CommandLine.Option(names = "--persons", description = "Path to the list of persons to consider")
    private String relevantPersonsFile;

    @Override
    public Integer call() throws Exception {
        new KelheimAutoTuning(configFile, outputFolder, referenceDataFile, targetError, maxRunningTime,
                patience, relevantPersonsFile).calibrate();
        return 0;
    }

    public static void main(String[] args) throws IOException {
        new RunKelheimAutoTuning().execute(args);
    }

    private static class KelheimAutoTuning extends AutomaticScenarioCalibrator {
        public KelheimAutoTuning(String configFile, String outputFolder, String referenceDataFile, double targetError,
                                 long maxRunningTime, int patience, String relevantPersonsFile) throws IOException {
            super(configFile, outputFolder, referenceDataFile, targetError, maxRunningTime, patience, relevantPersonsFile);
        }

        @Override
        public void runSimulation() {
            File inputConfigFile = new File(configFile);
            String temporaryConfigPath = inputConfigFile.getParent() + "/auto-tune.config.xml";
            ConfigUtils.writeConfig(config, temporaryConfigPath);
            Config singleUseConfig = ConfigUtils.loadConfig(temporaryConfigPath);
            prepareConfig(singleUseConfig);
            Scenario scenario = ScenarioUtils.loadScenario(singleUseConfig);
            prepareScenario(scenario);
            Controler controler = new Controler(scenario);
            prepareController(controler);
            controler.run();
        }

        private void prepareConfig(Config config) {
            for (long ii = 600; ii <= 97200; ii += 600) {

                for (String act : List.of("home", "restaurant", "other", "visit", "errands",
                        "educ_higher", "educ_secondary", "educ_primary", "educ_tertiary", "educ_kiga", "educ_other")) {
                    config.planCalcScore()
                            .addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act + "_" + ii).setTypicalDuration(ii));
                }

                config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work_" + ii).setTypicalDuration(ii)
                        .setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
                config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("business_" + ii).setTypicalDuration(ii)
                        .setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
                config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("leisure_" + ii).setTypicalDuration(ii)
                        .setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));

                config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_daily_" + ii).setTypicalDuration(ii)
                        .setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
                config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_other_" + ii).setTypicalDuration(ii)
                        .setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
            }

            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("car interaction").setTypicalDuration(60));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("other").setTypicalDuration(600 * 3));

            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("freight_start").setTypicalDuration(60 * 15));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("freight_end").setTypicalDuration(60 * 15));

            config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info);
            config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
        }

        private void prepareScenario(Scenario scenario) {
            for (Link link : scenario.getNetwork().getLinks().values()) {
                Set<String> modes = link.getAllowedModes();
                // allow freight traffic together with cars
                if (modes.contains("car")) {
                    HashSet<String> newModes = Sets.newHashSet(modes);
                    newModes.add("freight");

                    link.setAllowedModes(newModes);
                }
            }
        }

        private void prepareController(Controler controler) {
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    install(new KelheimPtFareModule());
                    install(new SwissRailRaptorModule());
                    bind(AnalysisMainModeIdentifier.class).to(KelheimMainModeIdentifier.class);
                    addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);
                    addControlerListenerBinding().to(StrategyWeightFadeout.class).in(Singleton.class);
                    Multibinder<StrategyWeightFadeout.Schedule> schedules = Multibinder.newSetBinder(binder(), StrategyWeightFadeout.Schedule.class);
                    schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode, "person", 0.75, 0.85));
                    bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
                }
            });
        }

    }
}

