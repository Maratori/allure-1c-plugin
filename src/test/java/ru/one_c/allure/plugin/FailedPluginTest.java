package ru.one_c.allure.plugin;

import io.qameta.allure.DefaultLaunchResults;
import io.qameta.allure.Issue;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.entity.Time;
import io.qameta.allure.tree.Tree;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static io.qameta.allure.entity.LabelName.PACKAGE;
import static io.qameta.allure.entity.LabelName.TEST_METHOD;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class FailedPluginTest {

    @Test
    public void shouldCreateTree() throws Exception {
        final Set<TestResult> testResults = new HashSet<>();

        final TestResult first = new TestResult()
                .setName("first")
                .setLabels(asList(PACKAGE.label("a.b"), TEST_METHOD.label("firstMethod")));
        final TestResult second = new TestResult()
                .setName("second")
                .setLabels(singletonList(PACKAGE.label("a.c")));
        testResults.add(first);
        testResults.add(second);

        final LaunchResults results = new DefaultLaunchResults(
                testResults,
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        final FailedPlugin failedPlugin = new FailedPlugin();
        final Tree<TestResult> tree = failedPlugin.getData(singletonList(results));

        Assertions.assertThat(tree.getChildren())
                .hasSize(1)
                .extracting("name")
                .containsExactlyInAnyOrder("a");

        Assertions.assertThat(tree.getChildren())
                .flatExtracting("children")
                .extracting("name")
                .containsExactlyInAnyOrder("b", "c");

        Assertions.assertThat(tree.getChildren())
                .flatExtracting("children")
                .flatExtracting("children")
                .extracting("name")
                .containsExactlyInAnyOrder("firstMethod", "second");
    }

    @Test
    public void shouldFilterByStatus() throws Exception {
        final Set<TestResult> testResults = new HashSet<>();

        final TestResult first = new TestResult()
                .setName("first")
                .setLabels(asList(PACKAGE.label("a.b"), TEST_METHOD.label("firstMethod")))
                .setStatus(Status.PASSED);
        final TestResult second = new TestResult()
                .setName("second")
                .setLabels(singletonList(PACKAGE.label("a.c")))
                .setStatus(Status.FAILED);
        final TestResult third = new TestResult()
                .setName("third")
                .setLabels(singletonList(PACKAGE.label("a.d")))
                .setStatus(Status.FAILED);
        testResults.add(first);
        testResults.add(second);
        testResults.add(third);

        final LaunchResults results = new DefaultLaunchResults(
                testResults,
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        final FailedPlugin failedPlugin = new FailedPlugin();
        final Tree<TestResult> tree = failedPlugin.getData(singletonList(results));

        Assertions.assertThat(tree.getChildren())
                .hasSize(1)
                .extracting("name")
                .containsExactlyInAnyOrder("a");

        Assertions.assertThat(tree.getChildren())
                .flatExtracting("children")
                .extracting("name")
                .containsExactlyInAnyOrder("c", "d");

        Assertions.assertThat(tree.getChildren())
                .flatExtracting("children")
                .flatExtracting("children")
                .extracting("name")
                .containsExactlyInAnyOrder("second", "third");
    }

    @Test
    public void shouldCollapseNodesWithOneChild() throws Exception {
        final Set<TestResult> testResults = new HashSet<>();

        final TestResult first = new TestResult()
                .setName("first")
                .setLabels(singletonList(PACKAGE.label("a.b.c")));
        final TestResult second = new TestResult()
                .setName("second")
                .setLabels(singletonList(PACKAGE.label("a.d.e")));
        testResults.add(first);
        testResults.add(second);

        final LaunchResults results = new DefaultLaunchResults(
                testResults,
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        final FailedPlugin failedPlugin = new FailedPlugin();
        final Tree<TestResult> tree = failedPlugin.getData(singletonList(results));

        Assertions.assertThat(tree.getChildren())
                .hasSize(1)
                .extracting("name")
                .containsExactlyInAnyOrder("a");

        Assertions.assertThat(tree.getChildren())
                .flatExtracting("children")
                .extracting("name")
                .containsExactlyInAnyOrder("b.c", "d.e");

        Assertions.assertThat(tree.getChildren())
                .flatExtracting("children")
                .flatExtracting("children")
                .extracting("name")
                .containsExactlyInAnyOrder("first", "second");
    }

    @Issue("531")
    @Test
    public void shouldProcessTestsInNestedPackages() throws Exception {
        final Set<TestResult> testResults = new HashSet<>();
        final TestResult first = new TestResult()
                .setName("first")
                .setLabels(singletonList(PACKAGE.label("a.b")));
        final TestResult second = new TestResult()
                .setName("second")
                .setLabels(singletonList(PACKAGE.label("a.b.c")));

        testResults.add(first);
        testResults.add(second);

        final LaunchResults results = new DefaultLaunchResults(
                testResults,
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        final FailedPlugin failedPlugin = new FailedPlugin();
        final Tree<TestResult> tree = failedPlugin.getData(singletonList(results));

        Assertions.assertThat(tree.getChildren())
                .hasSize(1)
                .extracting("name")
                .containsExactlyInAnyOrder("a.b");

        Assertions.assertThat(tree.getChildren())
                .flatExtracting("children")
                .extracting("name")
                .containsExactlyInAnyOrder("first", "c");

        Assertions.assertThat(tree.getChildren())
                .flatExtracting("children")
                .filteredOn("name", "c")
                .flatExtracting("children")
                .extracting("name")
                .containsExactlyInAnyOrder("second");
    }

    @Issue("587")
    @Issue("572")
    @Test
    public void shouldSortByStartTimeAsc() throws Exception {
        final TestResult first = new TestResult()
                .setName("first")
                .setTime(new Time().setStart(10L));
        final TestResult second = new TestResult()
                .setName("second")
                .setTime(new Time().setStart(100L));
        final TestResult third = new TestResult()
                .setName("third")
                .setTime(new Time().setStart(50L));
        final TestResult timeless = new TestResult()
                .setName("timeless");

        final LaunchResults results = new DefaultLaunchResults(
                new HashSet<>(Arrays.asList(first, second, third, timeless)),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        final FailedPlugin failedPlugin = new FailedPlugin();
        final Tree<TestResult> tree = failedPlugin.getData(singletonList(results));

        Assertions.assertThat(tree.getChildren())
                .extracting("name")
                .containsExactly("timeless", "first", "third", "second");
    }
}