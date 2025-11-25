package com.siseg.util;

import com.siseg.model.enumerations.TipoVeiculo;

/**
 * Constantes relacionadas a veículos e simulação de entrega
 */
public class VehicleConstants {
    
    public static final double VELOCIDADE_MEDIA_BICICLETA_KMH = 20.0;
    public static final double VELOCIDADE_MEDIA_MOTO_CARRO_KMH = 45.0;
    
    public static final double FATOR_VARIACAO_VELOCIDADE = 0.3;
    public static final double FATOR_DESVIO_VELOCIDADE = 0.15;
    public static final double FATOR_VELOCIDADE_MINIMA = 0.7;
    public static final double FATOR_VELOCIDADE_MAXIMA = 1.3;
    
    public static final double SEGUNDOS_POR_HORA = 3600.0;
    public static final double INTERVALO_SIMULACAO_SEGUNDOS = 10.0;
    public static final double FATOR_ACELERACAO_SIMULACAO = 1.0;
    
    public static final int TEMPO_PADRAO_ENTREGA_MINUTOS = 30;
    public static final int TEMPO_MAXIMO_ENTREGA_MINUTOS = 120;
    public static final int TEMPO_MINIMO_ENTREGA_MINUTOS = 1;
    
    public static final String OSRM_PROFILE_CYCLING = "cycling";
    public static final String OSRM_PROFILE_DRIVING = "driving";
    
    private VehicleConstants() {
    }
    
    public static double getVelocidadeMediaKmh(TipoVeiculo tipoVeiculo) {
        if (tipoVeiculo == TipoVeiculo.BICICLETA) {
            return VELOCIDADE_MEDIA_BICICLETA_KMH;
        }
        return VELOCIDADE_MEDIA_MOTO_CARRO_KMH;
    }
    
    public static String getOsrmProfile(TipoVeiculo tipoVeiculo) {
        if (tipoVeiculo == TipoVeiculo.BICICLETA) {
            return OSRM_PROFILE_CYCLING;
        }
        return OSRM_PROFILE_DRIVING;
    }
}

