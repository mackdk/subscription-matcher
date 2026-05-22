FROM registry.opensuse.org/opensuse/leap:16.0

# Installing all packages
RUN zypper refresh && \
        zypper -n install gawk bash-completion curl expect git man openssh sudo tar vim wget \
                          nodejs22 java-17-openjdk-devel maven jq ShellCheck && \
         zypper clean -a

# Creating regular user
RUN useradd --create-home --shell /bin/bash matcher
RUN echo "matcher ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

USER matcher
