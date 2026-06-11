export default class extends Extension {
  req(url) {
    return this.request(url, { headers: { "Miru-Url": "https://jimov-api.vercel.app" } });
  }
  async latest(page) {
    const res = await this.req(`/anime/tioanime/filter?page=${page}`);
    const results = Array.isArray(res) ? res : ((res && res.results) || []);
    return results.map((item) => ({ url: item.url, title: item.name, cover: item.image || "" }));
  }
  async search(kw, page) {
    const res = await this.req(`/anime/tioanime/filter?q=${encodeURIComponent(kw)}&page=${page}`);
    const results = Array.isArray(res) ? res : ((res && res.results) || []);
    return results.map((item) => ({ title: item.name, url: item.url, cover: item.image || "" }));
  }
  async detail(url) {
    const res = await this.req(url);
    if (!res) return { title: "", cover: "", desc: "", episodes: [] };
    const cover = res.image ? (res.image.url || res.image) : "";
    const episodes = (res.episodes || []).map((ep) => ({
      name: ep.name || `Ep ${ep.num || ep.number || "?"}`,
      url: ep.url,
    }));
    return { title: res.name || "", cover, desc: res.synopsis || "", episodes: [{ title: "Episodios", urls: episodes }] };
  }
  async watch(url) {
    return { type: "hls", url: "error://unsupported-player" };
  }
}
