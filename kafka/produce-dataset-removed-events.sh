#!/bin/bash

schema_id="4"
messages='{ "type": "DATASET_REMOVED", "fdkId": "6467f35c-f246-48f5-956c-616b949e52e6", "timestamp": 1647698567000, "graph": "" }\n
{ "type": "DATASET_REMOVED", "fdkId": "6467f35c-f246-48f5-956c-616b949e52e2", "timestamp": 1647698516000, "graph": "" }'

docker-compose exec schema-registry bash -c "echo '$messages'|kafka-avro-console-producer --bootstrap-server kafka:29092 --topic dataset-events --property value.schema.id=${schema_id}"
