package com.digitalasset.testing.junit4;

import com.digitalasset.testing.utils.Preconditions;
import com.google.common.base.Strings;

public class MavenCredentials {
  private final String userName;
  private final String password;

  private MavenCredentials(String userName, String password) {
    this.userName = userName;
    this.password = password;
  }

  public String getUserName() {
    return userName;
  }

  public String getPassword() {
    return password;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String userName;
    private String password;

    private Builder() {}

    public Builder userName(String userName) {
      this.userName = userName;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public MavenCredentials build() {
      Preconditions.require(
          !Strings.isNullOrEmpty(userName),
          "a user name is required when building maven credentials");
      Preconditions.require(
          !Strings.isNullOrEmpty(password),
          "a password is required when building maven credentials");
      return new MavenCredentials(userName, password);
    }
  }
}
