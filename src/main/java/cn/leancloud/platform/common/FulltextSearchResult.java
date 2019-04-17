package cn.leancloud.platform.common;

import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * result for fulltext search.
 */
public class FulltextSearchResult {
  private List<JsonObject> results;

  private int hits;
  private String sid;

  public FulltextSearchResult() {
    ;
  }

  public List<JsonObject> getResults() {
    return results;
  }

  public void setResults(List<JsonObject> results) {
    this.results = results;
  }

  public int getHits() {
    return hits;
  }

  public void setHits(int hits) {
    this.hits = hits;
  }

  public String getSid() {
    return sid;
  }

  public void setSid(String sid) {
    this.sid = sid;
  }
}
