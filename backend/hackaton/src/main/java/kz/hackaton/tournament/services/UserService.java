package kz.hackaton.tournament.services;


import kz.hackaton.tournament.dto.JwtRequest;
import kz.hackaton.tournament.entities.Role;
import kz.hackaton.tournament.entities.User;
import kz.hackaton.tournament.repositories.RoleRepository;
import kz.hackaton.tournament.repositories.UserRepository;
import kz.hackaton.tournament.responses.BodyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public User findUserByLogin(String username) {
        return userRepository.findUserByLogin(username).orElseThrow(() -> new UsernameNotFoundException("User not found by " + username));
    }




    @Override
    @Transactional
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = findUserByLogin(login);
        return new org.springframework.security.core.userdetails.User(user.getLogin(), user.getPassword(), mapRolesToAuthorities(user.getRoles()));
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
    }

    public BodyResponse register(JwtRequest jwtRequest) {
        Optional<User> user = userRepository.findUserByLogin(jwtRequest.getLogin());
        if (user.isPresent()) {
            return new BodyResponse("User exists", Response.Status.CONFLICT, null);
        }

        User savedUser = new User();
        savedUser.setLogin(jwtRequest.getLogin());
        savedUser.setPassword(passwordEncoder.encode(jwtRequest.getPassword()));
        savedUser.setName(jwtRequest.getName());
        savedUser.setSurname(jwtRequest.getSurname());
        savedUser.setMajor(jwtRequest.getMajor());
        Collection<Role> collection = new ArrayList<>();
        collection.add(roleRepository.findById(1L).get());
        savedUser.setRoles(collection);

        return new BodyResponse("User created", Response.Status.OK, userRepository.save(savedUser));
    }
}
