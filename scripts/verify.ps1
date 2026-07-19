$ErrorActionPreference = "Stop"

Push-Location "$PSScriptRoot\..\backend"
try {
    mvn test
}
finally {
    Pop-Location
}

Push-Location "$PSScriptRoot\..\frontend"
try {
    npm run build
}
finally {
    Pop-Location
}

