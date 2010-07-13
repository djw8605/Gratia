package GratiaReporting::Reporter;

use strict;
require Exporter;

use vars qw(@ISA @EXPORT @EXPORT_OK);

@ISA = qw(Exporter);

@EXPORT = qw(&invoke);
@EXPORT_OK = qw();

sub new {
  my $class = shift;
  my $self = { options => { @_ } };
  bless $self, $class;
  return $self;
}

sub invoke($\@\@) {
  my ($self, $command, $mail_opts) = @_;
  $mail_opts = [] unless $mail_opts;
  my @escaped_command = map { my $cmd = $_; $cmd =~ s&"&\\"&g; sprintf('"%s"', $cmd) } @$command, @$mail_opts;
  if ($self->{options}->{debug}) {
    print "Would execute ",
      join(" ", @escaped_command), "\n";
  } else {                      # For real
    if ($self->{options}->{test}) {
      unshift @escaped_command, "set -x;";
    } else {
      push @escaped_command, ">/dev/null 2>&1";
    }
    system(join(" ", @escaped_command));
  }
}

sub usage {
  my $fh = shift;
  print $fh <<EOF;
General options:

  --debug
   -d

    Don't actually execute the reports, just state what would happen.

  --verbose
   -v

    Be more verbose.
EOF
}


1;

__END__

### Local Variables:
### mode: cperl
### End:
