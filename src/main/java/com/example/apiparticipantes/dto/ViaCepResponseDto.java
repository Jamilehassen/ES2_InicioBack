package com.example.apiparticipantes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ignora campos desconhecidos que o ViaCEP possa retornar
@JsonIgnoreProperties(ignoreUnknown = true)
public class ViaCepResponseDto {

    private String cep;
    private String logradouro;
    private String complemento; // Mesmo que não usemos, mapeamos para evitar erros
    private String bairro;
    private String localidade; // Cidade
    private String uf;         // Estado (Sigla)
    private String ibge;       // Código IBGE (opcional)
    private String gia;        // (opcional)
    private String ddd;        // (opcional)
    private String siafi;      // (opcional)
    private boolean erro;      // ViaCEP retorna erro=true se o CEP não for encontrado

    // --- Getters e Setters para todos os campos ---
    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getLocalidade() {
        return localidade;
    }

    public void setLocalidade(String localidade) {
        this.localidade = localidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getIbge() {
        return ibge;
    }

    public void setIbge(String ibge) {
        this.ibge = ibge;
    }

    public String getGia() {
        return gia;
    }

    public void setGia(String gia) {
        this.gia = gia;
    }

    public String getDdd() {
        return ddd;
    }

    public void setDdd(String ddd) {
        this.ddd = ddd;
    }

    public String getSiafi() {
        return siafi;
    }

    public void setSiafi(String siafi) {
        this.siafi = siafi;
    }

    public boolean isErro() {
        return erro;
    }

    public void setErro(boolean erro) {
        this.erro = erro;
    }
}