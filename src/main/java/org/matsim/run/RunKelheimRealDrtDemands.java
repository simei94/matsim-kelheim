package org.matsim.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.av.maxspeed.DvrpTravelTimeWithMaxSpeedLimitModule;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.drtFare.KelheimDrtFareModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;


@CommandLine.Command(
        name = "run-real-demand",
        description = "Run the DRT real demands (drt-only plans)"
)
public class RunKelheimRealDrtDemands implements MATSimAppCommand {
    private static final Logger log = LogManager.getLogger(RunKelheimRealDrtDemands.class);

    @CommandLine.Option(names = "--config", description = "path to config file", required = true)
    private String configFile;

    @CommandLine.Option(names = "--network-change-events", description = "path to network change events file (for simulating the traffic in the network)", defaultValue = "")
    private String networkChangeEventsFile;

    @CommandLine.Option(names = "--dates", description = "date of the actual request to run (yyyymmdd, separated with ,)", defaultValue = "")
    private String dates;

    public static void main(String[] args) {
        new RunKelheimRealDrtDemands().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        String[] datesToRun = new String[]{"20201126"};
        if (!dates.equals("")) {
            datesToRun = dates.split(",");
        }
        for (String date : datesToRun) {
            Config config = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
            config.plans().setInputFile("./real-drt-demands/" + date + "-drt.plans.xml");
            log.info("Setting input plans file to: " + config.plans().getInputFile());

//            String outputDirectory = Path.of(config.controler().getOutputDirectory()).getParent().toString() + "/" + date;
//            config.controler().setOutputDirectory(outputDirectory);
            log.info("Setting output directory to: " + config.controler().getOutputDirectory());

            config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info);
            config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
            if (!networkChangeEventsFile.equals("")) {
                config.network().setTimeVariantNetwork(true);
                config.network().setChangeEventsInputFile(networkChangeEventsFile);
            }
            config.controler()
                    .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

            Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
            ScenarioUtils.loadScenario(scenario);
            Network network = scenario.getNetwork();

            // Adding DRT modules
            Controler controler = new Controler(scenario);
            controler.addOverridingModule(new MultiModeDrtModule());
            controler.addOverridingModule(new DvrpModule());
            controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(config)));
            MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
            for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
                if (drtCfg.getMode().equals("av")){
                    VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("autonomousVehicleType", VehicleType.class ) );
                    vehicleType.setMaximumVelocity(5); // 18km/h --> 5m/s
                    controler.addOverridingModule(new DvrpTravelTimeWithMaxSpeedLimitModule(vehicleType));
                }
                controler.addOverridingModule(new KelheimDrtFareModule(drtCfg, network));
            }
            controler.run();
        }
        return 0;
    }
}
