// Jenkins Job DSL — List views

listView('All Dev Jobs') {
    description('All dev environment jobs')
    includeRegex('dev.*')
    columns {
        status(); weather(); name(); lastSuccess(); lastFailure(); lastDuration(); buildButton()
    }
}

listView('All Prod Jobs') {
    description('All prod environment jobs')
    includeRegex('prod.*')
    columns {
        status(); weather(); name(); lastSuccess(); lastFailure(); lastDuration(); buildButton()
    }
}

listView('Shared Infrastructure') {
    description('Shared infra provisioning jobs')
    includeRegex('shared-infra-creation/.*')
    columns {
        status(); weather(); name(); lastSuccess(); lastFailure(); lastDuration(); buildButton()
    }
}

listView('All Application Pipelines') {
    description('All app CI/CD pipelines')
    includeRegex('(dev|prod)/(catalog-api|inventory-api|customer-api|order-api|notification-api|ui).*')
    columns {
        status(); weather(); name(); lastSuccess(); lastFailure(); lastDuration(); buildButton()
    }
}
