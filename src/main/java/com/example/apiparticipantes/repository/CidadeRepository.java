package com.example.apiparticipantes.repository;

import com.example.apiparticipantes.model.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CidadeRepository extends JpaRepository<Cidade, String> {
}
