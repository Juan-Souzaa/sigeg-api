package com.siseg.util;

import com.siseg.model.enumerations.TipoVeiculo;

/**
 * Constantes relacionadas a veículos e simulação de entrega
 */
public class VehicleConstants {
    
    // Velocidades médias em km/h
    public static final double VELOCIDADE_MEDIA_BICICLETA_KMH = 15.0;
    public static final double VELOCIDADE_MEDIA_MOTO_CARRO_KMH = 30.0;
    
    // Fatores de variação de velocidade na simulação
    public static final double FATOR_VARIACAO_VELOCIDADE = 0.4;
    public static final double FATOR_DESVIO_VELOCIDADE = 0.2;
    public static final double FATOR_VELOCIDADE_MINIMA = 0.5;
    public static final double FATOR_VELOCIDADE_MAXIMA = 1.5;
    
    // Configurações de simulação
    public static final double SEGUNDOS_POR_SEGUNDO = 3600.0; // Conversão de horas para segundos
    public static final double INTERVALO_SIMULACAO_SEGUNDOS = 10.0; // Intervalo real entre execuções (10 segundos)
    public static final double FATOR_ACELERACAO_SIMULACAO = 5.0; // Acelera simulação: 1 segundo real = 5 segundos simulados
    
    // Tempos padrão de entrega
    public static final int TEMPO_PADRAO_ENTREGA_MINUTOS = 30;
    public static final int TEMPO_MAXIMO_ENTREGA_MINUTOS = 120;
    public static final int TEMPO_MINIMO_ENTREGA_MINUTOS = 1;
    
    // Profiles OSRM
    public static final String OSRM_PROFILE_CYCLING = "cycling";
    public static final String OSRM_PROFILE_DRIVING = "driving";
    
    private VehicleConstants() {
        // Utility class
    }
    
    /**
     * Obtém a velocidade média base para um tipo de veículo
     */
    public static double getVelocidadeMediaKmh(TipoVeiculo tipoVeiculo) {
        if (tipoVeiculo == TipoVeiculo.BICICLETA) {
            return VELOCIDADE_MEDIA_BICICLETA_KMH;
        }
        return VELOCIDADE_MEDIA_MOTO_CARRO_KMH;
    }
    
    /**
     * Obtém o profile OSRM apropriado para um tipo de veículo
     */
    public static String getOsrmProfile(TipoVeiculo tipoVeiculo) {
        if (tipoVeiculo == TipoVeiculo.BICICLETA) {
            return OSRM_PROFILE_CYCLING;
        }
        return OSRM_PROFILE_DRIVING;
    }
}

