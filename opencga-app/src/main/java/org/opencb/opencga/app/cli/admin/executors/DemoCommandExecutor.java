/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.app.cli.admin.executors;


import org.opencb.opencga.app.cli.CommandExecutor;
import org.opencb.opencga.app.cli.admin.AdminCliOptionsParser;
import org.opencb.opencga.app.demo.DemoManager;
import org.opencb.opencga.app.demo.config.DemoConfiguration;

import java.nio.file.Paths;

/**
 * Created by imedina on 02/03/15.
 */
public class DemoCommandExecutor extends CommandExecutor {

    private AdminCliOptionsParser.DemoCommandOptions demoCommandOptions;

    public DemoCommandExecutor(AdminCliOptionsParser.DemoCommandOptions demoCommandOptions) {
        super(demoCommandOptions.commonOptions);
        this.demoCommandOptions = demoCommandOptions;
    }

    @Override
    public void execute() throws Exception {
        logger.debug("Executing variant command line");

        String subCommandString = demoCommandOptions.getParsedSubCommand();
        switch (subCommandString) {
            case "load":
                load();
                break;
            case "add":
                add();
                break;
            case "delete":
                delete();
                break;
            default:
                logger.error("Subcommand not valid");
                break;
        }

    }

    private void load() throws Exception {
        System.out.println("Load!!");

        AdminCliOptionsParser.LoadDemoCommandOptions loadDemoCommandOptions = demoCommandOptions.loadDemoCommandOptions;

        DemoConfiguration demoConfiguration = DemoConfiguration.load(Paths.get("build/conf/main.yml"));
        DemoManager demoManager = new DemoManager(demoConfiguration, clientConfiguration);

    }

    private void add() throws Exception {

    }

    private void delete() throws Exception {

    }
}
