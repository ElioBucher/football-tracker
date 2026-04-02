let currentTeamId = null;
let currentTeamData = null;
let liveRefreshInterval = null;
let refreshCountdown = 60;
let favorites = [];

async function initFavorites() {
    try {
        favorites = JSON.parse(localStorage.getItem('football-favorites') || '[]');
    } catch {
        favorites = [];
    }
}

const teamRegistry = new Map();

function registerTeam(id, name, crest) {
    teamRegistry.set(id, {id, name, crest: crest || ''});
}

function getTeam(id) {
    const fromRegistry = teamRegistry.get(id);
    if (fromRegistry) return fromRegistry;

    const fromFavorite = favorites.find(f => f.id === id);
    if (fromFavorite) {
        registerTeam(fromFavorite.id, fromFavorite.name, fromFavorite.crest);
        return teamRegistry.get(id);
    }
    return {id, name: 'Unbekanntes Team', crest: ''};
}

async function toggleTheme() {
    const isDark = document.body.classList.toggle('dark');
    localStorage.setItem('football-theme', isDark ? 'dark' : 'light');
    updateThemeIcon();
}

function updateThemeIcon() {
    const btn = document.querySelector('.theme-toggle');
    if (!btn) return;
    const isDark = document.body.classList.contains('dark');
    btn.innerHTML = isDark
        ? `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z"/></svg>`
        : `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>`;
}

function showPage(page) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('page-' + page).classList.add('active');

    const navIndex = {live: 0, today: 1, standings: 2, scorers: 3, search: 4, favorites: 5};
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    if (navIndex[page] !== undefined) {
        const btn = document.querySelectorAll('.nav-btn')[navIndex[page]];
        if (btn) btn.classList.add('active');
    }

    if (liveRefreshInterval) {
        clearInterval(liveRefreshInterval);
        liveRefreshInterval = null;
    }

    if (page === 'live') {
        loadLive();
        startLiveRefresh();
    } else if (page === 'today') {
        loadToday();
    } else if (page === 'standings') {
        const first = document.querySelector('#standings-pills .pill.active') || document.querySelector('#standings-pills .pill');
        loadStandings('PL', first);
    } else if (page === 'scorers') {
        const first = document.querySelector('#scorers-pills .pill.active') || document.querySelector('#scorers-pills .pill');
        loadScorers('PL', first);
    } else if (page === 'search') {
        renderFavorites();
    } else if (page === 'favorites') {
        renderFavorites();
    }
}

async function favoriteTeam() {
    if (!currentTeamId) return;
    const name = currentTeamData?.name || getTeam(currentTeamId).name || String(currentTeamId);
    const crest = currentTeamData?.crest || getTeam(currentTeamId).crest || '';
    toggleFavorite(currentTeamId, name, crest);
}

function startLiveRefresh() {
    refreshCountdown = 60;
    updateCountdown();
    liveRefreshInterval = setInterval(() => {
        refreshCountdown--;
        updateCountdown();
        if (refreshCountdown <= 0) {
            loadLive();
            refreshCountdown = 60;
        }
    }, 1000);
}

function updateCountdown() {
    const el = document.getElementById('refresh-countdown');
    if (el) el.textContent = `Update in ${refreshCountdown}s`;
}

function showSkeleton(containerId, count = 5) {
    document.getElementById(containerId).innerHTML =
        Array(count).fill('<div class="skeleton"></div>').join('');
}

function getDisplayScore(score, side) {
    if (!score) return '-';

    const fullTime = score.fullTime?.[side];
    const regularTime = score.regularTime?.[side];
    const halfTime = score.halfTime?.[side];

    if (fullTime !== null && fullTime !== undefined) return fullTime;
    if (regularTime !== null && regularTime !== undefined) return regularTime;
    if (halfTime !== null && halfTime !== undefined) return halfTime;
    return '-';
}

function renderMatches(matches, containerId) {
    const el = document.getElementById(containerId);
    if (!matches || matches.length === 0) {
        el.innerHTML = '<div class="empty">Keine Spiele gefunden</div>';
        return;
    }

    el.innerHTML = matches.map(m => {
        const isLive = m.status === 'IN_PLAY' || m.status === 'PAUSED' || m.status === 'LIVE';
        const htHome = m.score?.halfTime?.home;
        const htAway = m.score?.halfTime?.away;
        const hasHT = htHome !== null && htHome !== undefined && htAway !== null && htAway !== undefined;
        const goalsHtml = m.goals && m.goals.length > 0
            ? '<div class="goals-list">' + m.goals.map(g =>
            `<span class="goal-item">${g.minute ?? ''}' ${g.scorer?.name ?? ''} (${g.team?.name ?? ''})</span>`
        ).join('') + '</div>'
            : '';
        const competitionHtml = m.competition
            ? `<div class="match-competition">${m.competition.name}</div>`
            : '';

        return `
            <div class="match-card ${isLive ? 'live-card' : ''}" onclick="openMatchDetail(${m.id})">
                <div class="team-side team-home">
                    <div class="team-name">${m.homeTeam?.name ?? '-'}</div>
                    ${m.homeTeam?.crest ? `<img class="team-crest-sm" src="${m.homeTeam.crest}" onerror="this.style.display='none'"/>` : ''}
                </div>
                <div class="score-box">
                    <div class="score">
                        ${getDisplayScore(m.score, 'home')} : ${getDisplayScore(m.score, 'away')}
                    </div>
                    <div class="match-meta">
                        ${isLive
            ? '<span class="live-badge">LIVE</span>'
            : new Date(m.utcDate).toLocaleString('de-CH', {
                day: '2-digit',
                month: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            })
        }
                    </div>
                    ${hasHT ? `<div class="halftime-score">HZ ${htHome}:${htAway}</div>` : ''}
                    ${competitionHtml}
                </div>
                <div class="team-side team-away">
                    ${m.awayTeam?.crest ? `<img class="team-crest-sm" src="${m.awayTeam.crest}" onerror="this.style.display='none'"/>` : ''}
                    <div class="team-name">${m.awayTeam?.name ?? '-'}</div>
                </div>
            </div>
            ${goalsHtml ? `<div class="goals-wrapper">${goalsHtml}</div>` : ''}
        `;
    }).join('');
}

async function loadLive() {
    showSkeleton('live-content', 4);
    try {
        const res = await fetch('/api/football/live');
        if (!res.ok) throw new Error();
        const data = await res.json();
        renderMatches(data, 'live-content');
    } catch {
        document.getElementById('live-content').innerHTML = '<div class="empty">Fehler beim Laden</div>';
    }
}

async function loadToday() {
    showSkeleton('today-content', 5);
    try {
        const res = await fetch('/api/football/today');
        if (!res.ok) throw new Error();
        const data = await res.json();
        renderMatches(data, 'today-content');
    } catch {
        document.getElementById('today-content').innerHTML = '<div class="empty">Fehler beim Laden</div>';
    }
}

function renderStandingsTable(table, totalTeams) {
    return `
        <div class="table-wrapper">
            <table class="standings-table">
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Team</th>
                        <th>Sp</th>
                        <th>S</th>
                        <th>U</th>
                        <th>N</th>
                        <th>Tore</th>
                        <th>TD</th>
                        <th>Pkt</th>
                        <th>Form</th>
                    </tr>
                </thead>
                <tbody>
                    ${table.map(t => {
        let zoneClass = '';
        if (t.position <= 4) zoneClass = 'cl-zone';
        else if (t.position <= 6) zoneClass = 'el-zone';
        else if (t.position > totalTeams - 3) zoneClass = 'rel-zone';

        const form = (t.form || '').split(',').filter(Boolean).slice(-5);
        const formHtml = form.map(f => `<span class="form-dot form-${f}">${f}</span>`).join('');

        return `
                            <tr class="${zoneClass}">
                                <td class="pos">${t.position}</td>
                                <td>
                                    <img class="team-crest" src="${t.team?.crest || ''}" onerror="this.style.display='none'"/>
                                    ${t.team?.name || '-'}
                                </td>
                                <td>${t.playedGames ?? 0}</td>
                                <td>${t.won ?? 0}</td>
                                <td>${t.draw ?? 0}</td>
                                <td>${t.lost ?? 0}</td>
                                <td>${t.goalsFor ?? 0}:${t.goalsAgainst ?? 0}</td>
                                <td>${(t.goalDifference ?? 0) > 0 ? '+' : ''}${t.goalDifference ?? 0}</td>
                                <td class="points">${t.points ?? 0}</td>
                                <td><div class="form-cell">${formHtml}</div></td>
                            </tr>
                        `;
    }).join('')}
                </tbody>
            </table>
        </div>
    `;
}

async function loadStandings(leagueId, el) {
    document.querySelectorAll('#standings-pills .pill').forEach(p => p.classList.remove('active'));
    if (el) el.classList.add('active');
    document.getElementById('standings-content').innerHTML = '<div class="loading">Lade Tabelle…</div>';

    try {
        const res = await fetch('/api/football/standings/' + leagueId);
        if (!res.ok) throw new Error();
        const data = await res.json();
        const standings = Array.isArray(data.standings) ? data.standings : [];

        if (!standings.length) {
            document.getElementById('standings-content').innerHTML = '<div class="empty">Keine Tabellendaten verfügbar</div>';
            return;
        }

        const totalStanding = standings.find(s => s.type === 'TOTAL' && Array.isArray(s.table) && s.table.length);
        let html = '';

        if (totalStanding) {
            html += renderStandingsTable(totalStanding.table, totalStanding.table.length);
        } else {
            html += standings
                .filter(s => Array.isArray(s.table) && s.table.length)
                .map((s, index) => `
                    <div style="margin-bottom:24px">
                        <div class="position-group-title">${s.group || `Gruppe ${index + 1}`}</div>
                        ${renderStandingsTable(s.table, s.table.length)}
                    </div>
                `)
                .join('');
        }

        html += `
            <div class="table-legend">
                <span><span class="legend-dot cl"></span>Champions League</span>
                <span><span class="legend-dot el"></span>Europa League</span>
                <span><span class="legend-dot rel"></span>Abstieg</span>
            </div>
        `;

        document.getElementById('standings-content').innerHTML = html;
    } catch {
        document.getElementById('standings-content').innerHTML = '<div class="empty">Fehler beim Laden</div>';
    }
}

async function loadScorers(leagueId, el) {
    document.querySelectorAll('#scorers-pills .pill').forEach(p => p.classList.remove('active'));
    if (el) el.classList.add('active');
    document.getElementById('scorers-content').innerHTML = '<div class="loading">Lade Torjäger…</div>';

    try {
        const res = await fetch('/api/football/scorers/' + leagueId);
        if (!res.ok) throw new Error();
        const data = await res.json();
        const scorers = data.scorers || [];

        if (!scorers.length) {
            document.getElementById('scorers-content').innerHTML = '<div class="empty">Keine Daten verfügbar</div>';
            return;
        }

        document.getElementById('scorers-content').innerHTML = scorers.map((s, i) => `
            <div class="scorer-card">
                <div class="scorer-rank">${i + 1}</div>
                <img class="scorer-crest" src="${s.team?.crest || ''}" onerror="this.style.display='none'"/>
                <div class="scorer-info">
                    <div class="scorer-name">${s.player?.name || '-'}</div>
                    <div class="scorer-team">${s.team?.name || ''}</div>
                </div>
                <div style="text-align:right">
                    <div class="scorer-goals">${s.goals ?? 0}</div>
                    <div class="scorer-assists">${s.assists ?? 0} Vorlagen</div>
                </div>
            </div>
        `).join('');
    } catch {
        document.getElementById('scorers-content').innerHTML = '<div class="empty">Fehler beim Laden</div>';
    }
}

async function searchTeam() {
    const name = document.getElementById('search-input').value.trim();
    if (!name) return;

    document.getElementById('search-content').innerHTML = '<div class="loading">Suche…</div>';

    try {
        const res = await fetch('/api/football/teams/search?name=' + encodeURIComponent(name));
        if (!res.ok) throw new Error();
        const data = await res.json();

        if (!data || data.length === 0) {
            document.getElementById('search-content').innerHTML = '<div class="empty">Kein Team gefunden</div>';
            return;
        }

        data.forEach(t => registerTeam(t.id, t.name, t.crest));

        document.getElementById('search-content').innerHTML = data.map(t => `
            <div class="team-card" data-team-id="${t.id}" onclick="openTeamById(${t.id})">
                <img class="team-crest-big" src="${t.crest || ''}" onerror="this.style.display='none'"/>
                <div class="team-info">
                    <h3>${t.name}</h3>
                    <p>${t.venue || ''} ${t.founded ? '· Gegründet ' + t.founded : ''}</p>
                    <p>${t.clubColors || ''}</p>
                </div>
                <svg class="team-card-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
            </div>
        `).join('');
    } catch {
        document.getElementById('search-content').innerHTML = '<div class="empty">Fehler beim Laden</div>';
    }
}

function openTeamById(id) {
    const team = getTeam(id);
    openTeam(team.id, team.name, team.crest);
}

async function toggleFavorite(id, name, crest) {
    const idx = favorites.findIndex(f => f.id === id);
    if (idx >= 0) {
        favorites.splice(idx, 1);
    } else {
        favorites.push({id, name, crest});
    }
    localStorage.setItem('football-favorites', JSON.stringify(favorites));
    registerTeam(id, name, crest);
    renderFavorites();
    updateFavButton(id);
}

function isFavorite(id) {
    return favorites.some(f => f.id === id);
}

function updateFavButton(id) {
    const btn = document.getElementById('fav-btn-' + id);
    if (!btn) return;

    const fav = isFavorite(id);
    btn.classList.toggle('is-fav', fav);
    btn.textContent = fav ? 'Favorit' : 'Favorisieren';
}

function renderFavorites() {
    ['favorites-section', 'favorites-page-section'].forEach(secId => {
        const sec = document.getElementById(secId);
        if (!sec) return;
        if (!favorites.length) {
            sec.innerHTML = '<div class="empty">Keine Favoriten gespeichert.</div>';
            return;
        }
        favorites.forEach(f => registerTeam(f.id, f.name, f.crest));
        sec.innerHTML = `
            <div class="favorites-label">Gespeicherte Teams</div>
            <div class="favorites-bar">
                ${favorites.map(f => {
            const safeName = f.name.replace(/'/g, "\\'");
            const safeCrest = (f.crest || '').replace(/'/g, "\\'");
            return `
                        <div class="fav-chip" onclick="openTeamById(${f.id})">
                            <img src="${f.crest || ''}" onerror="this.style.display='none'"/>
                            ${f.name}
                            <span class="fav-remove" onclick="event.stopPropagation();toggleFavorite(${f.id}, '${safeName}', '${safeCrest}')">
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                            </span>
                        </div>
                    `;
        }).join('')}
            </div>
        `;
    });
}

async function openTeam(id, name, crest) {
    currentTeamId = id;
    registerTeam(id, name, crest);
    const isFav = isFavorite(id);

    const safeName = name.replace(/'/g, "\\'");
    const safeCrest = (crest || '').replace(/'/g, "\\'");

    document.getElementById('team-header').innerHTML = `
        <div class="team-detail-header">
            <img class="team-detail-crest" src="${crest || ''}" onerror="this.style.display='none'"/>
            <div style="flex:1">
                <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin-bottom:8px">
                    <h2>${name}</h2>
                    <button id="fav-btn-${id}" class="fav-action-btn ${isFav ? 'is-fav' : ''}"
                        onclick="toggleFavorite(${id}, '${safeName}', '${safeCrest}')">
                        ${isFav ? 'Gespeichert' : 'Speichern'}
                    </button>
                </div>
                <div class="team-detail-meta" id="team-detail-meta">Lade Details…</div>
            </div>
        </div>`;

    showPage('team');
    switchTeamTab('matches', document.querySelector('#page-team .tabs .tab'));
    loadTeamMatches('SCHEDULED', document.querySelector('#page-team .liga-pills .pill'));

    try {
        const res = await fetch('/api/football/teams/' + id);
        if (res.ok) {
            const d = await res.json();
            currentTeamData = d;

            if (d.crest) registerTeam(id, d.name || name, d.crest);

            document.getElementById('team-detail-meta').innerHTML = `
                ${d.venue ? `<span class="meta-item">${d.venue}</span>` : ''}
                ${d.founded ? `<span class="meta-item">Gegründet ${d.founded}</span>` : ''}
                ${d.clubColors ? `<span class="meta-item">${d.clubColors}</span>` : ''}
                ${d.website ? `<span class="meta-item"><a href="${d.website}" target="_blank" style="color:var(--accent)">${d.website}</a></span>` : ''}
                ${d.coach?.name ? `<span class="meta-item">Trainer: ${d.coach.name}${d.coach.nationality ? ' · ' + d.coach.nationality : ''}</span>` : ''}
            `;
        } else {
            document.getElementById('team-detail-meta').innerHTML = 'Details nicht verfügbar';
        }
    } catch {
        document.getElementById('team-detail-meta').innerHTML = 'Details nicht verfügbar';
    }
}

function switchTeamTab(tab, el) {
    document.querySelectorAll('#page-team .tab').forEach(t => t.classList.remove('active'));
    if (el) el.classList.add('active');

    document.getElementById('team-tab-matches').style.display = tab === 'matches' ? 'block' : 'none';
    document.getElementById('team-tab-squad').style.display = tab === 'squad' ? 'block' : 'none';

    if (tab === 'squad') renderSquad();
}

function renderSquad() {
    const el = document.getElementById('squad-content');
    if (!currentTeamData || !currentTeamData.squad || !currentTeamData.squad.length) {
        el.innerHTML = '<div class="empty">Keine Kaderdaten verfügbar</div>';
        return;
    }

    const posOrder = ['Goalkeeper', 'Defence', 'Midfield', 'Offence'];
    const posLabel = {
        Goalkeeper: 'Torhüter',
        Defence: 'Abwehr',
        Midfield: 'Mittelfeld',
        Offence: 'Sturm'
    };

    const grouped = {};
    for (const pos of posOrder) grouped[pos] = [];

    for (const p of currentTeamData.squad) {
        const pos = p.position || 'Offence';
        if (!grouped[pos]) grouped[pos] = [];
        grouped[pos].push(p);
    }

    el.innerHTML = posOrder.map(pos => {
        const players = grouped[pos];
        if (!players.length) return '';

        return `
            <div class="position-group-title">${posLabel[pos] || pos}</div>
            <div class="squad-grid">
                ${players.map(p => `
                    <div class="player-card">
                        <div class="player-name">${p.name}</div>
                        <div class="player-pos">${p.position || ''}</div>
                        <div class="player-nat">${p.nationality || ''} ${p.dateOfBirth ? '· ' + p.dateOfBirth.substring(0, 4) : ''}</div>
                    </div>
                `).join('')}
            </div>
        `;
    }).join('');
}

async function loadTeamMatches(status, el) {
    document.querySelectorAll('#page-team .liga-pills .pill').forEach(p => p.classList.remove('active'));
    if (el) el.classList.add('active');

    showSkeleton('team-content', 5);

    try {
        const res = await fetch(`/api/football/teams/${currentTeamId}/matches?status=${status}`);
        if (!res.ok) throw new Error();
        const data = await res.json();
        renderMatches(data, 'team-content');
    } catch {
        document.getElementById('team-content').innerHTML = '<div class="empty">Fehler beim Laden</div>';
    }
}

async function openMatchDetail(id) {
    document.getElementById('modal-content').innerHTML = '<div class="loading">Lade Spieldetails…</div>';
    document.getElementById('match-modal').classList.add('open');

    try {
        const res = await fetch('/api/football/matches/' + id);
        if (!res.ok) throw new Error();

        const m = await res.json();
        const goals = m.goals || [];
        const htHome = m.score?.halfTime?.home;
        const htAway = m.score?.halfTime?.away;

        document.getElementById('modal-content').innerHTML = `
            <div class="modal-competition">
                ${m.competition?.name || ''} ${m.matchday ? '· Spieltag ' + m.matchday : ''}
            </div>
            <div class="modal-teams">
                <div class="modal-team">
                    <img src="${m.homeTeam?.crest || ''}" onerror="this.style.display='none'"/>
                    <div class="modal-team-name">${m.homeTeam?.name || '-'}</div>
                </div>
                <div class="modal-score">
                    ${getDisplayScore(m.score, 'home')} : ${getDisplayScore(m.score, 'away')}
                </div>
                <div class="modal-team">
                    <img src="${m.awayTeam?.crest || ''}" onerror="this.style.display='none'"/>
                    <div class="modal-team-name">${m.awayTeam?.name || '-'}</div>
                </div>
            </div>
            ${htHome !== null && htHome !== undefined && htAway !== null && htAway !== undefined
            ? `<div class="modal-ht">Halbzeit: ${htHome} : ${htAway}</div>`
            : ''}
            <div class="modal-date">
                ${new Date(m.utcDate).toLocaleString('de-CH', {
            weekday: 'long',
            day: '2-digit',
            month: 'long',
            hour: '2-digit',
            minute: '2-digit'
        })} · ${m.status || ''}
            </div>
            ${goals.length ? `
                <div class="modal-section-title">Tore</div>
                ${goals.map(g => `
                    <div class="goal-row">
                        <div class="goal-minute">${g.minute ?? ''}'</div>
                        <div style="flex:1">
                            <div>${g.scorer?.name || 'Unbekannt'}</div>
                            <div class="goal-type">${g.type || ''} · ${g.team?.name || ''}</div>
                        </div>
                    </div>
                `).join('')}
            ` : '<div class="modal-empty">Keine Tor-Details verfügbar</div>'}
        `;
    } catch {
        document.getElementById('modal-content').innerHTML = '<div class="empty">Details nicht verfügbar</div>';
    }
}

function closeModal(event) {
    if (event.target === document.getElementById('match-modal')) {
        document.getElementById('match-modal').classList.remove('open');
    }
}

(async () => {
    await initFavorites();
    if (localStorage.getItem('football-theme') === 'dark') {
        document.body.classList.add('dark');
    }
    updateThemeIcon();
    showPage('today');
})();