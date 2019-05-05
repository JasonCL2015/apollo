package com.ctrip.framework.apollo.biz.repository;

import com.ctrip.framework.apollo.biz.entity.Namespace;

import org.hibernate.annotations.Where;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import javax.persistence.SqlResultSetMapping;
import java.util.List;

public interface NamespaceRepository extends PagingAndSortingRepository<Namespace, Long> {

  @Query(value = "select * from Namespace where appId=?1 and clusterName=?2",nativeQuery = true)
  List<Namespace> findAllByAppIdAndClusterNameOrderByIdAsc(String appId, String clusterName);

  List<Namespace> findByAppIdAndClusterNameOrderByIdAsc(String appId, String clusterName);

  @Query(value = "select * from Namespace where appId=?1 and clusterName=?2 and namespaceName=?3",nativeQuery = true)
  Namespace findAllByAppIdAndClusterNameAndNamespaceName(String appId, String clusterName, String namespaceName);

  Namespace findByAppIdAndClusterNameAndNamespaceName(String appId, String clusterName, String namespaceName);

  @Modifying
  @Query("update Namespace set isdeleted=1,DataChange_LastModifiedBy = ?3 where appId=?1 and clusterName=?2")
  int batchDelete(String appId, String clusterName, String operator);

  List<Namespace> findByAppIdAndNamespaceNameOrderByIdAsc(String appId, String namespaceName);

  List<Namespace> findByNamespaceName(String namespaceName, Pageable page);

  int countByNamespaceNameAndAppIdNot(String namespaceName, String appId);

}
