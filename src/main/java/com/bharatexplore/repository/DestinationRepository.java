package com.bharatexplore.repository;

import com.bharatexplore.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    Optional<Destination> findByNameIgnoreCase(String name);

    @Query("""
            select d from Destination d
            where (:q is null or :q = ''
                   or lower(d.name) like lower(concat('%', :q, '%'))
                   or lower(d.state) like lower(concat('%', :q, '%'))
                   or lower(d.tags) like lower(concat('%', :q, '%')))
              and (:region is null or :region = '' or lower(d.region) = lower(:region))
            order by d.name
            """)
    List<Destination> search(@Param("q") String q, @Param("region") String region);
}
