package GratiaReporting::JobInfo;

use strict;
require Exporter;

use vars qw(@ISA @EXPORT @EXPORT_OK);

@ISA = qw(Exporter);

@EXPORT = qw();
@EXPORT_OK = qw();

1;

sub new {
  my $class = shift;
  my $parent = shift;
  my $self = { name => $parent?$parent->get_name():'' };
  bless $self, $class;
  $self->set_parent($parent);
  return $self;
}

sub add_to_key {
  my ($self, $key, $success, $value) = @_;
  return undef unless $key and defined $success and defined $value;
  return undef unless ($success == 0 or $success == 1);
  eval { $self->{parent}->set_dirty() } if $self->{parent};
  return ($self->{keys}->{$key}->{$success} += $value);
}

sub set_key {
  my ($self, $key, $success, $value) = @_;
  return undef unless $key and
    defined $success and
      ($success == 0 or $success == 1) and
        defined $value;
  return undef unless ($success == 0 or $success == 1);
  eval { $self->{parent}->set_dirty() } if $self->{parent};
  return ($self->{keys}->{$key}->{$success} = $value);
}

sub get_key {
  my ($self, $key, $success) = @_;
  return undef unless $key;
  if (defined $success) {
    return undef unless ($success == 0 or $success == 1);
    return $self->{keys}->{$key}->{$success} || 0;
  } else {
    return (($self->{keys}->{$key}->{0} || 0) + ($self->{keys}->{$key}->{1} || 0));
  }
}

sub get_keys {
  my $self = shift;
  return keys %{$self->{keys} || {}};
}

sub set_parent {
  my ($self, $ji) = @_;
  return undef unless $ji;
  return $self->{parent} = $ji;
}

sub get_parent {
  my $self = shift;
  return $self->{parent};
}

sub get_name {
  my $self = shift;
  return $self->get_name();
}

__END__

### Local Variables:
### mode: cperl
### End:
