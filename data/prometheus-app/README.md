# Cloud Native Computing Meetup Bern - Observability

This spring boot application is a demo application for the Application Monitoring with Prometheus talk at the 2. Cloud Native Meetup in Bern.

## Democase

* Expose Prometheus Actuator Metrics Endpoint 
* Custom Business Metric

## Functionality

Basic Idea of the application is a voting system who's looking forward to the apéro after the meetup and who's not.

the Votes are exposed as:
* Up-Votes: counter
* Down-Votes: counter
* Result Thumbs: Gauge

the prometheus Metrics can be scraped on the `/actuator/prometheus` endpoint

## License

see [LICENSE](LICENSE)

Uses JQuery Bar Rating Plugin, with MIT License: https://github.com/antennaio/jquery-bar-rating