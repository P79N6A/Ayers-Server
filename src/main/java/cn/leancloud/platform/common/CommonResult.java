package cn.leancloud.platform.common;

import java.util.List;

public class CommonResult {
  private List<Object> results;
  private int count;
  private String status;
  private String details;

  public CommonResult() {
    ;
  }

  public List<Object> getResults() {
    return results;
  }

  public void setResults(List<Object> results) {
    this.results = results;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }
}
