# wait for kafka to be ready to accept new topics
while ! /bin/kafka-topics --bootstrap-server localhost:9092 --list; do
    sleep 1
done

for topic in dataset-events data-service-events concept-events information-model-events service-events event-events rdf-parse-events; do
    kafka-topics --bootstrap-server localhost:9092 \
        --create --if-not-exists \
        --partitions 4 \
        --topic "${topic}"
done
