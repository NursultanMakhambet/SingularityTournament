package kz.hackaton.tournament.services;

import kz.hackaton.tournament.dto.*;
import kz.hackaton.tournament.entities.*;
import kz.hackaton.tournament.exceptions.TournamentException;
import kz.hackaton.tournament.repositories.TournamentRepositories;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.jni.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepositories tournamentRepositories;
    private final UserService userService;
    private final RoundService roundService;

    private final MatchService matchService;

    @Transactional
    public void joinTourney(String name, Long id) {
        Tournament tournament = tournamentRepositories.findById(id).orElseThrow(() -> new RuntimeException("a"));

        User user = userService.findUserByLogin(name);
        if (tournament.getUsers().contains(user)) {
            throw new TournamentException("You're already participant");
        }
        tournament.getUsers().add(user);
        // user.getTournaments().add(tournament);


    }

    @Transactional
    public void registerTourney(CreateTournamentDto createTournamentDto, String name) {
        List<Tournament> tournaments = tournamentRepositories.findAll();

        for (Tournament x : tournaments) {
            if (x.getType().equals(createTournamentDto.getType()) && !(x.getStatus().equals("completed")))
                throw new TournamentException("Tournament " + createTournamentDto.getType() + " already exists");
        }
//        if(tournaments != null) {
//            throw new TournamentException("Tournament " + createTournamentDto.getType() + " already exists");
//        }
        Tournament tournament = new Tournament();
        tournament.setName(createTournamentDto.getName());
        tournament.setType(createTournamentDto.getType());
        tournament.setCreatedDate(LocalDate.now());
        tournament.setStatus("registration");
        tournament.setDescription(createTournamentDto.getDescription());
        User owner = userService.findUserByLogin(name);
        tournament.setOwner(owner.getId());
        tournamentRepositories.save(tournament);
    }

    @Transactional
    public void startTourney(Long id, String login) {
        User userByLogin = userService.findUserByLogin(login);
        if (!userByLogin.getLogin().equals(login)) {
            System.out.println("ERORRRRRR");
            return;
        }

        Tournament tournament = tournamentRepositories.findById(id).orElseThrow(() -> new RuntimeException("EROR"));
        Collection<User> users = tournament.getUsers();
        int usersCount = users.size() - 1;
        int days = (usersCount / 5) * 2 + usersCount;
        tournament.setStatus("started");
        tournament.setStartedDate(LocalDate.now());
        tournament.setFinishedDate(tournament.getStartedDate().plusDays(days));

        List<Round> roundList = generateRound(tournament);

        tournament.setRoundList(roundList);

    }

    @Transactional
    public void winnerResult(WinnerResult winnerResult, String name) {

        User userFromLogin = userService.findUserByLogin(name);
        User userFromDto = userService.findUserBySurnameAndName(winnerResult.getSurname(), winnerResult.getName());

        Tournament tournament = tournamentRepositories.findById(winnerResult.getTournamentId()).get();
        LocalDate startedDate = tournament.getStartedDate();
        LocalDate now = LocalDate.now();
        if (ChronoUnit.DAYS.between(startedDate, now) + 1 != winnerResult.getStage()) {
            throw new TournamentException("You cannot change past/future details \n" +
                    "Days between : " + ChronoUnit.DAYS.between(now, startedDate) + "\n" +
                    "Stage : " + winnerResult.getStage());
        }
        List<Match> matchList = tournament.getRoundList().get(winnerResult.getStage() - 1).getMatchList();


        for (Match m : matchList) {
            if (m.getUser1().equals(userFromDto.getId()) || m.getUser2().equals(userFromDto.getId())) {
                if (m.getUser1().equals(userFromLogin.getId()) || m.getUser2().equals(userFromLogin.getId())) {
                    if (m.getWinner() != null) {

                        throw new TournamentException("Winner already exists");
                    }
                    m.setWinner(userFromDto.getId());
                    return;
                }
            }
        }
        throw new TournamentException("Error, Invalid data!");

    }

    @Transactional
    public TournamentBracketDto getDetailsTournamentBracket(Long id) {
        Tournament tournament = tournamentRepositories.findById(id).get();
        TournamentBracketDto tournamentBracketDto = new TournamentBracketDto(tournament.getId(), tournament.getName(), tournament.getType(), tournament.getDescription());
        List<Round> rounds = tournament.getRoundList();
        List<RoundDto> roundDtos = new ArrayList<>();
        for (Round round : rounds) {
            RoundDto roundDto = new RoundDto(round.getStage());
            List<Match> matchList = round.getMatchList();
            List<MatchDto> matchDtos = new ArrayList<>();
            for (Match match : matchList) {
                MatchDto matchDto = new MatchDto();
                matchDto.setUsername1(userService.getUserLogin(match.getUser1()));
                matchDto.setUsername2(userService.getUserLogin(match.getUser2()));
                if (match.getWinner() != null) {
                    matchDto.setWinner(userService.getUserLogin(match.getWinner()));
                }
                matchDtos.add(matchDto);
            }
            roundDto.setMatches(matchDtos);
            roundDtos.add(roundDto);
        }

        tournamentBracketDto.setRoundList(roundDtos);

        return tournamentBracketDto;
    }

    public TournamentFullDetailsDto getDetailsTournament(Long id) {
        Tournament tournament = tournamentRepositories.findById(id).get();
        TournamentFullDetailsDto tournamentFullDetailsDto = new TournamentFullDetailsDto(tournament.getId(), tournament.getName(),
                tournament.getType(), tournament.getDescription());
        List<User> users = (List<User>) tournament.getUsers();
        List<UserDto> collect = users.stream().map((u) -> new UserDto(u.getLogin(), u.getName(), u.getSurname(), u.getMajor())).collect(Collectors.toList());
        tournamentFullDetailsDto.setList(collect);
        return tournamentFullDetailsDto;

    }

    public List<RegisterTourneyDto> getRegisterTournaments(String status) {

        List<Tournament> tournaments = tournamentRepositories.findByStatus(status);
        LocalDate localDate = LocalDate.now();
        List<RegisterTourneyDto> list = new ArrayList<>();
        for (Tournament t : tournaments) {

            if (t.getFinishedDate() != null && ChronoUnit.DAYS.between(localDate, t.getFinishedDate()) <= 0) {
                t.setStatus("completed");
                continue;
            }
            RegisterTourneyDto registerTourneyDto = new RegisterTourneyDto();
            registerTourneyDto.setId(t.getId());
            registerTourneyDto.setName(t.getName());
            registerTourneyDto.setType(t.getType());
            registerTourneyDto.setParticipants(t.getUsers().size());
            registerTourneyDto.setDescription(t.getDescription());
            list.add(registerTourneyDto);
        }
        return list;
    }

    public List<LeaderBoardDto> getLeaderBoard(Long id) {
        List<TempLeaderBoardDto> list = tournamentRepositories.getLeaderBoard(id);
        Tournament tournament = tournamentRepositories.findById(id).get();
        Collection<User> users = tournament.getUsers();
        List<LeaderBoardDto> leaderBoardDtoList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if(list.get(i).getWinner() == null) {
                continue;
            }

            User user = userService.findById(list.get(i).getWinner());
            users.remove(user);
            LeaderBoardDto leaderBoardDto = new LeaderBoardDto(user.getName(), user.getSurname(), list.get(i).getCount());
            leaderBoardDtoList.add(leaderBoardDto);
        }
        for(User u : users) {
            LeaderBoardDto leaderBoardDto = new LeaderBoardDto(u.getName(), u.getSurname(), 0L);
            leaderBoardDtoList.add(leaderBoardDto);
        }



        return leaderBoardDtoList;

    }

    @Transactional
    public void info(InfoDto infoDto, String nameFromLogin) {
        User userByDto = userService.findUserBySurnameAndName(infoDto.getSurname(), infoDto.getName());
        if(userByDto == null) {
            throw new TournamentException("User not found");
        }
        UserProfile userProfile = userByDto.getUserProfile();
        if(userProfile == null) {
            userProfile = new UserProfile();
            List<String> facts = userProfile.getFacts();
            if(facts == null) {
                facts = new ArrayList<>();
                facts.add(infoDto.getFact());
            }

            List<String> done = userProfile.getDone();
            if(done == null) {
                done = new ArrayList<>();
                done.add(infoDto.getDone());
            }
            userProfile.setFacts(facts);
            userProfile.setDone(done);
            userProfile.setUser(userByDto);
            userByDto.setUserProfile(userProfile);

            return;
        }
        userByDto.getUserProfile().getFacts().add(infoDto.getFact());
        userByDto.getUserProfile().getDone().add(infoDto.getDone());
    }

    @Transactional
    public List<Round> generateRound(Tournament tournament) {
        List<User> users = (List<User>) tournament.getUsers();
        Collections.shuffle(users);
        List<Round> roundList = new ArrayList<>();
        int userCount = users.size();
        System.out.printf(users.toString());
        for (int i = 0; i < userCount - 1; i++) {
            Round round = new Round();
            round.setStage(i + 1);
            List<Match> matchList = new ArrayList<>();
            for (int j = 0; j < userCount / 2; j++) {
                Match match = new Match();
                match.setUser1(users.get(j).getId());
                match.setUser2(users.get(userCount - 1 - j).getId());
                matchService.save(match);
                matchList.add(match);

            }

            round.setMatchList(matchList);
            roundService.save(round);
            roundList.add(round);

            shuffleAlg(users);
        }
        return roundList;
    }


    public List<User> shuffleAlg(List<User> list) {
        User temp = list.get(list.size() - 1);
        for (int i = list.size() - 1; i > 0; i--) {
            if (i == 1) {
                list.set(i, temp);
                break;
            }
            list.set(i, list.get(i - 1));
        }
        return list;
    }

//    @Transactional
//    public void getDetailsTournamentLeaderBoard(Long id) {
//       Tournament tournament = tournamentRepositories.findById(id).orElseThrow(()-> new TournamentException("Tournament has not been found"));
//       List<User> users = (List<User>) tournament.getUsers();
//       List<Round> rounds = tournament.getRoundList();
//
//       for (Round round : rounds) {
//            List<Match> matches = round.getMatchList();
//            for(Match match : matches) {
//                match.getWinner();
//            }
//       }
//    }
}
