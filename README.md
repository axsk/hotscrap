# hotscrap

A script to scrap the hotslogs data and use that to assist in the picking phase.

## Draft of the counter function

1. Given a list of heroes sum over the specific counter values (obtained via "Win Rate vs Other Heroes")  against each of that hero.

2. Given a specific map, multiply that result with the map factor for each hero (obtained by comparing the overall win statistics with the map specific ones).

3. Given a specific playerid, multiply that result with the player factor for each hero (obtained by comparing the overall win statistics with the user ones). 

## Roadmap

* Allow reading in picks via screenshot (possible?)
* Analyze tuples of heroes
* Analyze classes (composition winrates, composition counters)
