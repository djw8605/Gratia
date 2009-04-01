package GratiaReporting::JobInfoBranch;

use strict;
require Exporter;

use GratiaReporting::JobInfo;

use vars qw(@ISA @EXPORT @EXPORT_OK
            $VERBOSITY
            $ERROR_LEVEL
            $INFO_LEVEL
            $FINE_LEVEL
            $DEBUG_LEVEL);

@ISA = qw(Exporter);

@EXPORT = qw();
@EXPORT_OK = qw();

$ERROR_LEVEL = -1;
$INFO_LEVEL = 0;
$FINE_LEVEL = 1;
$DEBUG_LEVEL = 2;

$VERBOSITY = 2;

1;

sub report {
  my ($self, $level, $fh, @message) = @_;
  print $fh join("", "JobInfoBranch: ", @message) if $self->{verbosity} > $level;
}

sub split_print_args(\@) {
  my $arglist_ref = shift;
  if (not ref ($arglist_ref->[0])) {
    unshift @$arglist_ref, \*STDERR;
  }
}

sub info {
  my $self = shift;
  split_print_args(@_);
  $self->report($INFO_LEVEL, @_);
}

sub error {
  my $self = shift;
  split_print_args(@_);
  my ($fh, @message) = @_;
  $self->report($ERROR_LEVEL, $fh, "ERROR: ", @message);
}

sub debug {
  my $self = shift;
  split_print_args(@_);
  $self->report($DEBUG_LEVEL, @_);
}

sub fine {
  my $self = shift;
  split_print_args(@_);
  $self->report($FINE_LEVEL, @_);
}

# New object.
sub new {
  my $class = shift;
  my $name = shift || '';
  my $parent = shift;
  my $self =
    { name => $name,
      summed_branches => {},
      alt_branches => {},
      verbosity => $VERBOSITY
    };
  bless $self, $class;
  $self->{totals} = new GratiaReporting::JobInfo($self);
  $self->set_parent($parent) if $parent;
  $self->debug("creating new branch ", $self->get_name(), "\n");
  return $self;
}

# Branch name
sub get_name {
  my $self = shift;
  return $self->{name} || '';
}

####################################
# Summed branches
sub summed_branch_exists {
  my ($self, $label) = @_;
  return exists $self->{summed_branches}->{$label};
}

sub get_summed_branches {
  my $self = shift;
  return $self->{summed_branches};
}

sub get_summed_branch {
  my ($self, $label) = @_;
  $self->{summed_branches}->{$label} =
    new GratiaReporting::JobInfoBranch($label, $self)
      unless $self->{summed_branches}->{$label};
    return $self->{summed_branches}->{$label};
}

sub add_summed_branch {
  my ($self, $ji) = @_;
  unless (defined $ji) {
    $self->error("tried to add undefined summed branch\n");
    return undef;
  }
  if (not ref $ji) { # Scalar
    my $label = $ji;
    if (exists $self->{summed_branches}->{$label}) {
      $self->error("tried to add summed branch with existing label $label\n");
      return undef;
    }
    $ji = $self->get_summed_branch("$label");
  } else {
    my $label = $ji->get_name();
    if (exists $self->{summed_branches}->{$label}) {
      $self->error("tried to add summed branch with existing label $label\n");
      return undef;
    }
    $self->{summed_branches}->{$label} = $ji;
  }
  return $ji;
}
####################################

####################################
# Alternative (non-summed) branches.
sub alt_branch_exists {
  my ($self, $label) = @_;
  return exists $self->{alt_branches}->{$label};
}

sub get_alt_branches {
  my $self = shift;
  return $self->{alt_branches};
}

sub get_alt_branch {
  my ($self, $label) = @_;
  $self->{alt_branches}->{$label} =
    new GratiaReporting::JobInfoBranch($label)
      unless $self->{alt_branches}->{$label};
    return $self->{alt_branches}->{$label};
}

sub add_alt_branch {
  my ($self, $ji) = @_;
  unless (defined $ji) {
    $self->error("tried to add undefined alt. branch\n");
    return undef;
  }
  my $label;
  if (not ref $ji) { # Scalar
    if (exists $self->{alt_branches}->{$label}) {
      $self->error("tried to add alt branch with existing label $label\n");
      return undef;
    }
    $label = $ji;
    $ji = $self->get_alt_branch("$label");
  } else {
    my $label = $ji->get_name();
    if (exists $self->{alt_branches}->{$label}) {
      $self->error("tried to add alt branch with existing label $label\n");
      return undef;
    }
    $self->{alt_branches}->{$label} = $ji;
  }
  return $ji;
}
####################################

# Get totals leaf.
sub get_leaf {
  my $self = shift;
  $self->recalc_totals() if $self->is_dirty();
  return $self->{totals};
}

# Obtain specified key from totals leaf
sub get_key {
  my ($self, $key, $success) = @_;
  return undef unless $key;
  if (defined $success) {
    return undef unless ($success == 0 or $success == 1);
    return $self->total($key, $success);
  }
}

# Calculate and return specified total
sub total {
  my ($self, $key, $success) = @_;
  return undef unless $key;
  if ($self->is_dirty()) {
    $self->recalc_totals();
    if ($self->is_dirty()) {
      error("recalc_totals was not able to reset dirty flag!\n");
      return undef;
    }
  }
  return $self->{totals}->get_key($key, $success);
}

sub set_dirty {
  my $self = shift;
  return not not $self->{dirty_flag};
  eval { $self->{parent}->set_dirty() } if $self->{parent};
}

sub is_dirty {
  my $self = shift;
  return not not $self->{dirty_flag};
}

sub recalc_totals {
  my $self = shift;
  $self->fine("recalculating totals for branch ", $self->get_name());
  foreach my $branch ($self->get_summed_branches()) {
    foreach my $key ($branch->get_keys()) {
      foreach my $success (0, 1) {
        $self->{totals}->add_to_key($branch->get_leaf()->get_key($key, $success));
      }
    }
  }
  undef $self->{dirty_flag};
  return;
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


__END__

### Local Variables:
### mode: cperl
### End:
