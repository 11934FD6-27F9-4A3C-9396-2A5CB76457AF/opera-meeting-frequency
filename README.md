Operational Journal Read Me

* Run är en “körning” ( ett steg)
* Journal är information om en Run
* QueueCheckpoint är metadata för omkörningar för en specifik Run
* MostRecentJournal är en Vy. Den plockar ut den senaste statusen för varje RunID. Så den har samma fält som Journal, men har bara en rad per Run ID. Så där kan man lätt se om en Run är Started (dvs. Running), Success eller Error