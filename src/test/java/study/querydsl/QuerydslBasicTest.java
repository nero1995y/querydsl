package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
@DisplayName("기본 문법")
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL() {
        //member1를 찾아라.
        String qlString = "select m from Member m " +
                "where m.username = :username";

        Member findByMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findByMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl() {
        QMember m = new QMember("m"); //엘리어스

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) //파라미터 바인딩 처리 ㄴ
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("Q-Type 활용")
    public void startQType() {

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("검색 조건 쿼리")
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        //.and(member.age.eq(10)))
                        .and(member.age.between(10, 30)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");


    }

    @Test
    @DisplayName("검색 조건 쿼리 and 방법")
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");


    }

    @Test
    @DisplayName("결과 조회")
    public void resultFetch() {
        // List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 단건
/*        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();*/

        // 처음 한건 조회
        Member fetchFirst = queryFactory
                .selectFrom(member)
                //.limit(1).fetchOne() 와 같음
                .fetchFirst();

        //페이징에서 사용
        // QueryResults<Member> results = queryFactory
        //        .selectFrom(member)
        //        .fetchResults();
        // 미지원! 별도 카운트 쿼리 만들어라 !

        //long count = queryFactory
        //        .selectFrom(member)
        //        .fetchCount();

        // 변경된 강의에 없는 코드
        Long totalCount = queryFactory
                .select(member.count())  // == count(member.id)
                .from(member)
                .fetchOne();

    }

    /*
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 올림차순 (asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(null last)
     * 5 > 6 > null
     *
     */
    @Test
    @DisplayName("정렬")
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    @DisplayName("페이징")
    public void paging1() {

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.age.desc())
                .offset(1)  //1부터시작
                .limit(2)   //2개를 가져옴
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("페이징 전체 조회 수가 필요하면? 지원중단 ")
    public void paging2() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.age.desc())
                .offset(1)  //1부터시작
                .limit(2)   //2개를 가져옴
                .fetchResults();
        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("집합")
    public void aggregation() {
        // Tuple 여러개 타입 꺼내올때를 지원
        List<Tuple> result = queryFactory.select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /*
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     * */
    @Test
    @DisplayName("집합 그룹바이")
    public void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);


        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // 10 + 20 /2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // 30 + 40 / 2
    }


    /*
     *  팀 A에 소속된 모든 회원
     * */
    @Test
    @DisplayName("조인 기본조인")
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    /*
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * 억지성 예제이긴하지만 모든 회원,팀을 가져와서 가능하다
     * */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

    }

    /*
     * 예) 회원과 팀을 조인하면서 , 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     * */
    @Test
    public void join_on_filtering() {
        queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();
    }

    /*
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.* , t.* FROM Member m LEFT JOIN Team On m.username = t.name
     * */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamA"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }

    }

    @PersistenceUnit
    EntityManagerFactory emf;


    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // loding entity d인지 확인 시켜주는애
        assertThat(loaded).as("폐치 조인 미적용").isFalse();
    }

    /*
     * 맴버를 조회하는데 다 가져오고 싶을떄
     * */
    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // loding entity d인지 확인 시켜주는애
        assertThat(loaded).as("폐치 조인 적용").isTrue();
    }

    /*
     *  나이가 가장 많은 회원 조회
     * */
    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");
        // 서브쿼리할때는 엘리어스를 다르게 !

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    /*
     * 나이가 평균 이상인 회
     * */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");
        // 서브쿼리할때는 엘리어스를 다르게 !

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    /*
     * 억지성 예제긴하지만 ! in절 10살초가
     * */
    @Test
    @DisplayName("In 절 서브쿼리")
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");
        // 서브쿼리할때는 엘리어스를 다르게 !

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);

    }
    /*
    *Select 절에서 서브쿼리 사용법
    * */
    @Test
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                ).from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple= " + tuple);
        }
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

}















