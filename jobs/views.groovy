// Jenkins Job DSL — List views

listView('All Dev Jobs') {
    description('All dev environment jobs')

    jobs {
        regex('dev.*')
    }

    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

listView('All Prod Jobs') {
    description('All prod environment jobs')

    jobs {
        regex('prod.*')
    }

    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

listView('Shared Infrastructure') {
    description('Shared infra provisioning jobs')

    jobs {
        regex('shared-infra-creation/.*')
    }

    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

listView('All Application Pipelines') {
    description('All app CI/CD pipelines')

    jobs {
        regex('(dev|prod)/(catalog-api|inventory-api|customer-api|order-api|notification-api|ui).*')
    }

    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}