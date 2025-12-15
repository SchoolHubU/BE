package com.shu.backend.domain.board.repository;

import com.shu.backend.domain.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findBySchoolId(Long schoolId);

    Optional<Board> findBySchoolIdAndTitle(Long schoolId, String boardTitle);
}
