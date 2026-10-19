package com.endruqq.imperium.game;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/** Random division of the arena into nations and provinces. */
public final class TerritoryGen {
    private TerritoryGen() {}

    /**
     * Replaces all nations/provinces with a random map.
     *
     * @param nationCount      2..16
     * @param provincesPerNation 1..12
     */
    public static void autoDivide(GameData d, int nationCount, int provincesPerNation, long seed) {
        RandomSource rnd = RandomSource.create(seed);
        int n = d.gridN;
        int total = nationCount * provincesPerNation;
        // keep players' nation membership if names are reused? We start fresh, but keep members of old nations in order.
        List<Nation> old = new ArrayList<>(d.nations.values());
        d.nations.clear();
        d.provinces.clear();
        d.buildings.clear();
        d.relations.clear();
        d.proposals.clear();
        Arrays.fill(d.cells, -1);

        // 1) province seeds, spread out (best-candidate sampling)
        List<int[]> seeds = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            int[] best = null;
            double bestDist = -1;
            for (int k = 0; k < 20; k++) {
                int x = 1 + rnd.nextInt(Math.max(1, n - 2));
                int z = 1 + rnd.nextInt(Math.max(1, n - 2));
                double dist = Double.MAX_VALUE;
                for (int[] s : seeds) dist = Math.min(dist, (s[0] - x) * (s[0] - x) + (s[1] - z) * (s[1] - z));
                if (dist > bestDist) {
                    bestDist = dist;
                    best = new int[]{x, z};
                }
            }
            seeds.add(best);
        }

        // 2) multi-source flood fill with random edge weights -> organic, connected provinces
        double[] dist = new double[n * n];
        Arrays.fill(dist, Double.MAX_VALUE);
        int[] owner = new int[n * n];
        Arrays.fill(owner, -1);
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        for (int i = 0; i < seeds.size(); i++) {
            int idx = seeds.get(i)[1] * n + seeds.get(i)[0];
            if (owner[idx] >= 0) continue;
            dist[idx] = 0;
            owner[idx] = i;
            pq.add(new double[]{0, idx, i});
        }
        double[] noise = new double[n * n];
        for (int i = 0; i < noise.length; i++) noise[i] = 0.6 + rnd.nextDouble() * 0.9;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!pq.isEmpty()) {
            double[] cur = pq.poll();
            int idx = (int) cur[1];
            int who = (int) cur[2];
            if (cur[0] > dist[idx] || owner[idx] != who) continue;
            int x = idx % n, z = idx / n;
            for (int[] dd : dirs) {
                int nx = x + dd[0], nz = z + dd[1];
                if (nx < 0 || nz < 0 || nx >= n || nz >= n) continue;
                int ni = nz * n + nx;
                double nd = cur[0] + noise[ni];
                if (nd < dist[ni]) {
                    dist[ni] = nd;
                    owner[ni] = who;
                    pq.add(new double[]{nd, ni, who});
                }
            }
        }

        // 3) province adjacency
        List<Set<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < total; i++) adj.add(new HashSet<>());
        for (int z = 0; z < n; z++) {
            for (int x = 0; x < n; x++) {
                int a = owner[z * n + x];
                if (a < 0) continue;
                if (x + 1 < n) {
                    int b = owner[z * n + x + 1];
                    if (b >= 0 && b != a) {
                        adj.get(a).add(b);
                        adj.get(b).add(a);
                    }
                }
                if (z + 1 < n) {
                    int b = owner[(z + 1) * n + x];
                    if (b >= 0 && b != a) {
                        adj.get(a).add(b);
                        adj.get(b).add(a);
                    }
                }
            }
        }

        // 4) nation capitals: farthest-point sampling over province seeds
        int[] nationOf = new int[total];
        Arrays.fill(nationOf, -1);
        List<Integer> capitals = new ArrayList<>();
        capitals.add(rnd.nextInt(total));
        while (capitals.size() < nationCount) {
            int best = -1;
            double bestD = -1;
            for (int i = 0; i < total; i++) {
                if (capitals.contains(i)) continue;
                double md = Double.MAX_VALUE;
                for (int c : capitals) {
                    int[] a = seeds.get(i), b = seeds.get(c);
                    md = Math.min(md, (a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]));
                }
                if (md > bestD) {
                    bestD = md;
                    best = i;
                }
            }
            capitals.add(best);
        }
        int[] sizes = new int[nationCount];
        for (int k = 0; k < nationCount; k++) {
            nationOf[capitals.get(k)] = k;
            sizes[k] = 1;
        }
        // 5) grow nations round-robin through adjacent provinces, nearest to their capital first
        boolean grew = true;
        while (grew) {
            grew = false;
            for (int k = 0; k < nationCount; k++) {
                if (sizes[k] >= provincesPerNation) continue;
                int best = -1;
                double bestD = Double.MAX_VALUE;
                int[] cap = seeds.get(capitals.get(k));
                for (int p = 0; p < total; p++) {
                    if (nationOf[p] != k) continue;
                    for (int q : adj.get(p)) {
                        if (nationOf[q] >= 0) continue;
                        int[] s = seeds.get(q);
                        double dd = (s[0] - cap[0]) * (s[0] - cap[0]) + (s[1] - cap[1]) * (s[1] - cap[1]);
                        if (dd < bestD) {
                            bestD = dd;
                            best = q;
                        }
                    }
                }
                if (best >= 0) {
                    nationOf[best] = k;
                    sizes[k]++;
                    grew = true;
                }
            }
        }
        // leftovers join any neighbouring nation (or stay wilderness if isolated)
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int p = 0; p < total; p++) {
                if (nationOf[p] >= 0) continue;
                for (int q : adj.get(p)) {
                    if (nationOf[q] >= 0) {
                        nationOf[p] = nationOf[q];
                        changed = true;
                        break;
                    }
                }
            }
        }

        // 6) create nations & provinces
        Set<String> takenNames = new HashSet<>();
        List<Nation> created = new ArrayList<>();
        for (int k = 0; k < nationCount; k++) {
            Nation nat = new Nation();
            nat.id = d.nextNationId++;
            Nation prev = k < old.size() ? old.get(k) : null;
            nat.name = NameGen.nation(rnd, takenNames);
            takenNames.add(nat.name);
            nat.colorIndex = k;
            if (prev != null) {
                nat.members.addAll(prev.members);
                nat.memberNames.addAll(prev.memberNames);
                nat.leader = prev.leader;
            }
            created.add(nat);
            d.nations.put(nat.id, nat);
        }
        Set<String> provNames = new HashSet<>();
        int[] provId = new int[total];
        for (int p = 0; p < total; p++) {
            Province pr = new Province();
            pr.id = d.nextProvinceId++;
            pr.name = NameGen.province(rnd, provNames);
            pr.owner = nationOf[p] >= 0 ? created.get(nationOf[p]).id : -1;
            pr.originalOwner = pr.owner;
            provId[p] = pr.id;
            d.provinces.put(pr.id, pr);
        }
        for (int k = 0; k < nationCount; k++) created.get(k).capitalProvince = provId[capitals.get(k)];
        for (int i = 0; i < owner.length; i++) d.cells[i] = owner[i] >= 0 ? provId[owner[i]] : -1;
        d.recomputeGeometry();
        d.markTerritoryChanged();
        d.markChanged();
    }

    /** Fills a list of cells with a province (admin brush). */
    public static void paint(GameData d, int provinceId, int[] cellIndices) {
        if (provinceId >= 0 && !d.provinces.containsKey(provinceId)) return;
        for (int i : cellIndices) {
            if (i >= 0 && i < d.cells.length) d.cells[i] = provinceId;
        }
        d.recomputeGeometry();
        d.markTerritoryChanged();
    }
}
