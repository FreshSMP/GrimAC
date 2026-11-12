package ac.grim.grimac.utils.team;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import lombok.Getter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class EntityTeam {

    public final String name;
    public final Set<String> entries = new HashSet<>();
    private final GrimPlayer player;
    @Getter
    private WrapperPlayServerTeams.CollisionRule collisionRule;

    public EntityTeam(GrimPlayer player, String name) {
        this.player = player;
        this.name = name;
    }

    public void update(WrapperPlayServerTeams teams) {
        teams.getTeamInfo().ifPresent(info -> this.collisionRule = info.getCollisionRule());

        final TeamHandler teamHandler = player.checkManager.get(TeamHandler.class);
        final WrapperPlayServerTeams.TeamMode mode = teams.getTeamMode();
        if (mode == WrapperPlayServerTeams.TeamMode.ADD_ENTITIES || mode == WrapperPlayServerTeams.TeamMode.CREATE) {
            for (String teamPlayer : teams.getPlayers()) {
                if (teamPlayer.equals(player.user.getName())) {
                    teamHandler.setPlayerTeam(this);
                    continue;
                }

                final UUID uuid = player.compensatedEntities.getProfileId(teamPlayer);
                if (uuid != null) {
                    teamHandler.addEntityToTeam(uuid.toString(), this);
                    continue;
                }

                teamHandler.addEntityToTeam(teamPlayer, this);
            }
        } else if (mode == WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES) {
            for (String teamPlayer : teams.getPlayers()) {
                if (teamPlayer.equals(player.user.getName())) {
                    // Player was removed from their team.
                    teamHandler.setPlayerTeam(null);
                    continue;
                }

                final UUID profileId = player.compensatedEntities.getProfileId(teamPlayer);
                if (profileId != null) {
                    String uuid = profileId.toString();
                    entries.remove(uuid);
                    teamHandler.removeEntityFromTeam(uuid);
                    continue;
                }

                // Entity was removed from their team.
                teamHandler.removeEntityFromTeam(teamPlayer);
                entries.remove(teamPlayer);
            }
        } else if (mode == WrapperPlayServerTeams.TeamMode.REMOVE) {

            EntityTeam playersTeam = teamHandler.getPlayerTeam();
            // The player's team was deleted, so we must unset the player's team
            if (playersTeam != null && playersTeam.name.equals(name)) {
                teamHandler.setPlayerTeam(null);
            }

            // Also remove the team set on entities
            for (String entry : entries) {
                teamHandler.removeEntityFromTeam(entry);
            }
            entries.clear();
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof EntityTeam t && Objects.equals(name, t.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
