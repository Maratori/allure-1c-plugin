package ru.one_c.allure.plugin;

import io.qameta.allure.Aggregator;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.LabelName;
import io.qameta.allure.entity.Parameter;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.tree.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.qameta.allure.Constants.DATA_DIR;
import static io.qameta.allure.entity.TestResult.comparingByTimeAsc;

public class ByHostPlugin implements Aggregator {

    private static final String JSON = "by_host.json";
    private static final String SEPARATOR = " - ";

    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final Path outputDirectory) throws IOException {
        final JacksonContext jacksonContext = configuration.requireContext(JacksonContext.class);
        final Path dataFolder = Files.createDirectories(outputDirectory.resolve(DATA_DIR));
        final Path dataFile = dataFolder.resolve(JSON);
        try (OutputStream os = Files.newOutputStream(dataFile)) {
            jacksonContext.getValue().writeValue(os, getData(launchesResults));
        }
    }

    Tree<TestResult> getData(final List<LaunchResults> launchResults) {
        final Tree<TestResult> failed = new TestResultTree(
                "failed",
                this::groupByOsHostPackagesAndParameters,
                new TestResultGroupFactory(),
                this::createLeaf
        );
        launchResults.stream()
                .map(LaunchResults::getAllResults)
                .flatMap(Collection::stream)
                .sorted(comparingByTimeAsc())
                .forEach(failed::add);
        return collapseGroupsWithOnlyOneChild(failed);
    }

    private List<TreeLayer> groupByOsHostPackagesAndParameters(final TestResult testResult) {
        List<TreeLayer> treeLayers = new ArrayList<>();
        String os = testResult
                .getParameters()
                .stream()
                .filter(parameter -> "os".equals(parameter.getName()))
                .map(Parameter::getValue)
                .findFirst()
                .orElse("Undefined");
        String host = testResult
                .findOneLabel(LabelName.HOST)
                .orElse("Undefined");
        treeLayers.add(new DefaultTreeLayer(os));
        treeLayers.add(new DefaultTreeLayer(host));
        Arrays.stream(testResult.getFullName().replace("?", "/?").split("[\\\\/]"))
                .map(DefaultTreeLayer::new)
                .forEach(treeLayers::add);
        return treeLayers;
    }

    private Tree<TestResult> collapseGroupsWithOnlyOneChild(final Tree<TestResult> packages) {
        packages.getChildren().stream()
                .filter(TestResultTreeGroup.class::isInstance)
                .map(TestResultTreeGroup.class::cast)
                .forEach(this::collapseGroupsWithOnlyOneChild);
        return packages;
    }

    private void collapseGroupsWithOnlyOneChild(final TestResultTreeGroup groupNode) {
        groupNode.getChildren().stream()
                .filter(TestResultTreeGroup.class::isInstance)
                .map(TestResultTreeGroup.class::cast)
                .forEach(this::collapseGroupsWithOnlyOneChild);
        final long count = groupNode.getChildren().stream()
                .filter(TestResultTreeGroup.class::isInstance)
                .count();
        if (groupNode.getChildren().size() == 1 && count == 1) {
            groupNode.getChildren().stream()
                    .filter(TestResultTreeGroup.class::isInstance)
                    .map(TestResultTreeGroup.class::cast)
                    .forEach(next -> {
                        final String name = groupNode.getName() + SEPARATOR + next.getName();
                        groupNode.setName(name);
                        groupNode.setUid(name);
                        groupNode.setChildren(next.getChildren());
                    });
        }
    }

    private TestResultTreeLeaf createLeaf(final TestResultTreeGroup parent, final TestResult testResult) {
        return new TestResultTreeLeaf(parent.getUid(), testResult);
    }
}
