package ru.one_c.allure.plugin;

import io.qameta.allure.Aggregator;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.LabelName;
import io.qameta.allure.entity.Status;
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
import java.util.stream.Collectors;

import static io.qameta.allure.Constants.DATA_DIR;
import static io.qameta.allure.entity.TestResult.comparingByTimeAsc;

public class FailedPlugin implements Aggregator {

    private static final String JSON = "failed.json";
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
                this::groupBySuitesPackagesAndParameters,
                new TestResultGroupFactory(),
                this::createLeaf
        );
        final List<Status> allowedStatuses = Arrays.asList(Status.FAILED, Status.BROKEN, Status.UNKNOWN);
        launchResults.stream()
                .map(LaunchResults::getAllResults)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(TestResult::getHistoryId))
                .entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(
                        testResult -> allowedStatuses.contains(testResult.getStatus())
                ))
                .flatMap(entry -> entry.getValue().stream())
                .sorted(comparingByTimeAsc())
                .forEach(failed::add);
        return collapseGroupsWithOnlyOneChild(failed);
    }

    private List<TreeLayer> groupBySuitesPackagesAndParameters(final TestResult testResult) {
        List<TreeLayer> treeLayers = new ArrayList<>();
        treeLayers.add(
                new DefaultTreeLayer(
                        testResult.findOneLabel("testsuite")
                                .filter(s -> !s.isEmpty())
                                .orElse("Other")
                )
        );
        Arrays.stream(testResult.getFullName().replace("?", "/?").split("[\\\\/]"))
                .map(DefaultTreeLayer::new)
                .forEach(treeLayers::add);
        if (!testResult.getParameters().isEmpty()) {
            treeLayers.add(
                    new DefaultTreeLayer(
                            String.format("{ %s }", String.join(", ", testResult.getParameterValues()))
                    )
            );
        }
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
        List<String> parameters = new ArrayList<>();
        testResult
                .findOneLabel(LabelName.HOST)
                .ifPresent(parameters::add);
        return new TestResultTreeLeaf(
                parent.getUid(),
                testResult.getName(),
                testResult.getUid(),
                testResult.getStatus(),
                testResult.getTime(),
                testResult.isFlaky(),
                parameters
        );
    }
}
