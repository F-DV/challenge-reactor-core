package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;


@DataMongoTest
public class CSVUtilTest {

    @Autowired
    public PlayerRepository playerRepository;

    private CsvUtilFile csvUtilFile;

    @BeforeEach
    void before(){
        this.csvUtilFile= new CsvUtilFile(playerRepository);
    }

    @Test
    void converterData(){
        List<Player> list = csvUtilFile.getPlayers();
        assert list.size() == 18207;
    }


    @Test
    void reactive_filtrarJugadoresMayoresA35(){
        List<Player> list = csvUtilFile.getPlayers();                           //Traemos lista
        Flux<Player> listFlux = Flux.fromStream(list.parallelStream()).cache(); //Creamos creamos flujo de datos de tipo flux
        Mono<Map<String, Collection<Player>>> listFilter = listFlux
                .filter(player -> player.age > 34)                             //filtramos de 35 o mayor
                .map(player -> {                                                //mapeamos la lista
                    player.name = player.name.toUpperCase(Locale.ROOT);
                    return player;                                              //devolvemos la lista mapeada
                })
                .buffer(100)
                .flatMap(playerA -> listFlux
                         .filter(playerB -> playerA.stream()
                                 .anyMatch(a ->  a.club.equals(playerB.club)))
                )
                .distinct()
                .collectMultimap(Player::getClub);

        assert listFilter.block().size() == 322;
    }

    @Test
    void reactive_filtrarJugadoresMayoresA34PorUnEquipoEspecifico(){
        List<Player> list = csvUtilFile.getPlayers();
        Flux<Player> listFlux = Flux.fromStream(list.parallelStream()).cache();
        Mono<Map<String, Collection<Player>>> listFilter = listFlux
                .filter(player -> player.age > 34 && player.club.equals("barcelona"))
                .map(player -> {
                    player.name = player.name.toUpperCase(Locale.ROOT);
                    return player;
                })
                .collectMultimap(Player::getClub);
        listFilter.block().forEach((equipo,players)->{
            System.out.println(equipo);
            players.stream().forEach(p-> System.out.println(p.name+"-"+p.age));
            assert players.size()==4;
        });
    }

    @Test
    void reactive_filtrarNacionalidadYRanking() {
        List<Player> list = csvUtilFile.getPlayers();
        Flux<Player> listFlux = Flux.fromStream(list.parallelStream()).cache();
        Mono<Map<String, Collection<Player>>> listFilter = listFlux
                .filter(player -> player.age == 27) //se añade el filter para que no impriman todos los datos debido a la cantidad
                .map(player -> {
                    player.name = player.name.toUpperCase(Locale.ROOT);
                    return player;
                })
                .sort((p,w)->w.winners-p.winners)
                .collectMultimap(Player::getNational);

        listFilter.block().forEach((national, players) -> {
            System.out.println("\n"+national);
            players.stream().forEach(p -> System.out.println(p.name + "- Partidos ganados: " +p.winners));
        });
    }
}
