'use strict';

allure.api.addTranslation('en', {
    tab: {
        paths: {
            name: 'Paths'
        }
    }
});

allure.api.addTranslation('ru', {
    tab: {
        paths: {
            name: 'Пути'
        }
    }
});

allure.api.addTab('paths', {
    title: 'tab.paths.name', icon: 'fa fa-align-left',
    route: 'paths(/)(:testGroup)(/)(:testResult)(/)(:testResultTab)(/)',
    onEnter: (function (testGroup, testResult, testResultTab) {
        return new allure.components.TreeLayout({
            testGroup: testGroup,
            testResult: testResult,
            testResultTab: testResultTab,
            tabName: 'tab.paths.name',
            baseUrl: 'paths',
            url: 'data/paths.json'
        });
    })
});

allure.api.addTranslation('en', {
    tab: {
        failed: {
            name: 'Failed'
        }
    }
});

allure.api.addTranslation('ru', {
    tab: {
        failed: {
            name: 'Падения'
        }
    }
});

allure.api.addTab('failed', {
    title: 'tab.failed.name', icon: 'fa fa-exclamation-triangle',
    route: 'failed(/)(:testGroup)(/)(:testResult)(/)(:testResultTab)(/)',
    onEnter: (function (testGroup, testResult, testResultTab) {
        return new allure.components.TreeLayout({
            testGroup: testGroup,
            testResult: testResult,
            testResultTab: testResultTab,
            tabName: 'tab.failed.name',
            baseUrl: 'failed',
            url: 'data/failed.json'
        });
    })
});

allure.api.addTranslation('en', {
    tab: {
        by_host: {
            name: 'By Host'
        }
    }
});

allure.api.addTranslation('ru', {
    tab: {
        by_host: {
            name: 'Машины'
        }
    }
});

allure.api.addTab('by_host', {
    title: 'tab.by_host.name', icon: 'fa fa-server',
    route: 'by_host(/)(:testGroup)(/)(:testResult)(/)(:testResultTab)(/)',
    onEnter: (function (testGroup, testResult, testResultTab) {
        return new allure.components.TreeLayout({
            testGroup: testGroup,
            testResult: testResult,
            testResultTab: testResultTab,
            tabName: 'tab.by_host.name',
            baseUrl: 'by_host',
            url: 'data/by_host.json'
        });
    })
});
