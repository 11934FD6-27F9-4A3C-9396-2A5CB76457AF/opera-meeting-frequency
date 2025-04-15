@description('Environment')
@allowed([
  'dev'
  'prod'
])
param environment string

@description('Function App Name')
param functionAppName string

@description('Function name')
var functionName = 'opera-operations-functions'

@description('Storage account name in "Opera Integration Development or Production"')
var storageAccountName = 'operastorageaccount${environment}'