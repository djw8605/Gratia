package ReportingData;

use strict;
require Exporter;

use XML::LibXML; # Parse XML file

use vars qw(@ISA @EXPORT @EXPORT_OK $vo_data $site_data $people_data @test_addresses);

@ISA = qw(Exporter);

@EXPORT = qw($vo_data $site_data $people_data @test_addresses);
@EXPORT_OK = qw();

my $parser = XML::LibXML->new();

sub new {
  my $class = shift;
  my $self = { options => { @_ } };
  bless $self, $class;
  $self->process_data();
  return $self;
}

sub process_data {
  my $self = shift;
  foreach my $data_source (@{$self->{options}->{data_source}}) {
    if ($self->{options}->{verbose}) {
      print STDERR "Reading from $data_source\n";
    }
    my $fh;
    if ($data_source =~ m&^(\w+)://&) { # Use wget to obtain XML.
      $fh = FileHandle->new("wget -q -O - \"$data_source\" 2>/dev/null|") or 
        die "Unable to open wget pipe for $data_source";
    } else {
      $fh = FileHandle->new("$data_source");
    }
    my $data_content = join("", <$fh>);
    if ($data_content =~ m&^\s*<&s) { # XML
      $self->processXmlData($data_source, $data_content);
    } else {                    # Assume Perl data file.
      $self->processPerlData($data_source, $data_content);
    }
  }
}

sub processXmlData {
  my ($self, $data_source, $data_content) = @_;
  my $tree;
  eval { $tree = $parser->parse_string($data_content) };
  if ($@) {
    print STDERR "ERROR: Problem reading $data_source:\n$@";
    next;
  }
  unless ($tree) {
    die "Unable to parse a tree from XML source";
  }
  my $root = $tree->getDocumentElement;
  if ($self->{options}->{verbose}) {
    print STDERR "Reading OIM ", $root->nodeName , " data\n";
  }
  if ($root->nodeName eq 'resource_contacts') {
    $self->processXmlResourceContacts($root); # OIM site info
  } elsif ($root->nodeName eq 'vo_contacts') {
#    $self->processXmlVoContacts($root); # OIM VO info
  } else {
    print STDERR "ERROR: OIM data ", $root->nodeName, " not recognized: ignoring.\n";
  }
}

sub processXmlVoContacts {
  my ($self, $root) = @_;
  foreach my $vo ($root->findnodes('vo')) { # Each vo
    my $vo_name = $vo->findvalue('vo_name');
    my $this_vo;
    if (exists $vo_data->{$vo_name}) {
      print STDERR "WARNING: info for $vo_name has already been seen: merging.\n";
    } else {
      $vo_data->{$vo_name} = {};
    }
    $this_vo = $vo_data->{$vo_name};
    $this_vo->{science_fields} = {} unless $this_vo->{science_fields};
    my $science_nodes = $vo->findnodes('vo_fields_of_science/vo_field_of_science');
    foreach my $science_node ($science_nodes->get_nodelist()) {
      $this_vo->{science_fields}->{$science_node->string_value()} = 1;
    }
    my $reporting_contact_nodes = $this_vo->findnodes('reporting_contact');
    foreach my $reporting_contact ($reporting_contact_nodes->get_nodelist()) {
      my $primary_email = $reporting_contact->findvalue('primary_email')
        || $reporting_contact->findvalue('alt_email'); # Contact's email
      next unless $primary_email;
      my $person = {};
      push @{$this_vo->{reporting_contacts}}, $primary_email;
      $person->{vos} = [ ] unless $person->{vos};
      push @{$person->{vos}}, "$vo";
      foreach my $attribute qw(first_name middle_name last_name) { # Name info
        $person->{$attribute} =
          $reporting_contact->findvalue($attribute);
      }
      $self->mergePersonData($primary_email, $person);
    }
    my $reporting_group_nodes = $vo->findnodes('vo_reporting_group');
    $this_vo->{reporting_groups} = {} unless $this_vo->{reporting_groups};
    foreach my $reporting_group_node ($reporting_group_nodes->get_nodelist()) {
      my $vo_reporting_name = $reporting_group_node->findvalue('vo_reporting_name');
      my $this_reporting_group = $this_vo->{reporting_groups}->{$vo_reporting_name};
      $this_reporting_group->{reporting_contacts} = []
        unless $this_reporting_group->{reporting_contacts};
      $this_vo->{reporting_groups}->{$vo_reporting_name} = {}
        unless $this_vo->{reporting_groups}->{$vo_reporting_name};
      my $reporting_contact_nodes = $reporting_group_node->findnodes('reporting_contact');
      foreach my $reporting_contact ($reporting_contact_nodes->get_nodelist()) {
        my $primary_email = $reporting_contact->findvalue('primary_email')
          || $reporting_contact->findvalue('alt_email'); # Contact's email
        next unless $primary_email;
        my $person = {};
        push @{$this_vo->{reporting_contacts}}, $primary_email;
        $person->{vo_reportingnames} = [ ] unless $person->{vo_reporting_names};
        push @{$person->{vo_reporting_names}}, "$vo/$vo_reporting_name";
        foreach my $attribute qw(first_name middle_name last_name) { # Name info
          $person->{$attribute} =
            $reporting_contact->findvalue($attribute);
        }
        $self->mergePersonData($primary_email, $person);
      }
    }
  }
}

sub processXmlResourceContacts {
  my ($self, $root) = @_;
  foreach my $resource ($root->findnodes('resource')) { # Each resource
    my $site_name = $resource->findvalue('resource_name'); # Resource's name
    my $this_site;
    if (exists $site_data->{$site_name}) {
      print STDERR "WARNING: info for $site_name has already been seen: merging.\n";
    } else {
      $site_data->{$site_name} = {};
    }
    $this_site = $site_data->{$site_name};
    $this_site->{grid_type} = $resource->findvalue('grid_type'); # Grid type for resource
    my $service_nodes = $resource->findnodes('service_list/service'); # Services
    $this_site->{services} = {} unless $this_site->{services};
    foreach my $service_node ($service_nodes->get_nodelist()) {
      $this_site->{services}->{$service_node->string_value()} =
        {
         hidden => $service_node->findvalue('@hidden')?1:0 };
    }
    $this_site->{owner_vos} = {} unless $this_site->{owner_vos};
    my $owner_nodes =  $resource->findnodes('vo_ownership/vo_owner');
    foreach my $owner_node ($owner_nodes->get_nodelist()) {
      $this_site->{owner_vos}->{$owner_node->findvalue('vo_name')} =
        $owner_node->findvalue('percent');
    }
    $this_site->{reporting_contacts} = [ ] unless $this_site->{reporting_contacts};
    #    next unless ($site_name and ($all_sites or $wanted_sites->{$site_name})); # Want this site?
    #    next if ($grid_type and not $wanted_grid_types->{$grid_type}); # Want this grid type?
    #    next if (scalar @services and not grep { $wanted_services->{$_} } @services); # Want these services?
    #    $sites->{$site_name} = 1 unless $site_name =~ m&^all$&i;
    my $reporting_contact_nodes = $resource->findnodes('.//reporting_contact');
    foreach my $reporting_contact ($reporting_contact_nodes->get_nodelist()) { # Each contact
      my $primary_email = $reporting_contact->findvalue('primary_email')
        || $reporting_contact->findvalue('alt_email'); # Contact's email
      next unless $primary_email;
      my $person = {};
      push @{$this_site->{reporting_contacts}}, $primary_email;
      $person->{sites} = [ ] unless $person->{sites};
      push @{$person->{sites}}, $site_name;
      foreach my $attribute qw(first_name middle_name last_name) { # Name info
        $person->{$attribute} =
          $reporting_contact->findvalue($attribute);
      }
      $self->mergePersonData($primary_email, $person);
    }
  }
}

sub processPerlData {
  my ($self, $data_source, $data_content) = @_;
  my %tmp_hash = eval $data_content;
  if ($@) {
    print STDERR "ERROR: Problem reading $data_source:\n$@";
    next;
  }
  @test_addresses = @{$tmp_hash{test}} if exists $tmp_hash{test};
  $self->ImportVOData($tmp_hash{vos}) if exists $tmp_hash{vos};
  $self->ImportPeopleData($tmp_hash{people}) if exists $tmp_hash{people};
}

sub ImportVOData {
  my ($self, $vo_in) = @_;
  foreach my $vo_name ( sort keys %$vo_in ) {
    my $vo = $vo_in->{$vo_name};
    $vo_data->{lc $vo_name} = {} unless exists $vo_data->{lc $vo_name};
    if (exists $vo_data->{lc $vo_name}->{alt_vos} and exists $vo->{alt_vos}) {
      push @{$vo_data->{lc $vo_name}->{alt_vos}}, @{$vo->{alt_vos}};
    } else {
      $vo_data->{lc $vo_name}->{alt_vos} = $vo->{alt_vos};
    }
  }
}

sub ImportPeopleData {
  my ($self, $people_data)= @_;
  foreach my $person_email (sort keys %$people_data) {
    $self->mergePersonData($person_email, $people_data->{$person_email});
  }
}

sub mergePersonData {
  my ($self, $primary_email, $person) = @_;
  if (exists $people_data->{$primary_email}) {
    # Merge
    foreach my $key (sort keys %$person) {
      if ($people_data->{$primary_email}->{$key}) {
        if (ref $people_data->{$primary_email}->{$key} eq 'ARRAY') {
          my %merging_hash = map { $_ => 1} @{$people_data->{$primary_email}->{$key}};
          grep { $merging_hash{$_} = 1 } @{$person->{$key}};
          @{$people_data->{$primary_email}->{$key}} = sort keys %merging_hash;
        } elsif (ref $people_data->{$primary_email}->{$key} eq 'HASH') {
          foreach my $subhash_key (keys %{$person->{$key}}) {
            $people_data->{$primary_email}->{$key}->{$subhash_key} =
              $person->{$key}->{$subhash_key} if $person->{$key}->{$subhash_key};
          }
        } else {
          $people_data->{$primary_email}->{$key} = $person->{$key} if $person->{$key};
        }
      } else {
        $people_data->{$primary_email}->{$key} = $person->{$key};
      }
    }
  } else {
    $people_data->{$primary_email} = $person;
  }
  # Update person's real name.
  my $full_name = sprintf("%s %s %s",
                          $people_data->{$primary_email}->{first_name} || "",
                          $people_data->{$primary_email}->{middle_name} || "",
                          $people_data->{$primary_email}->{last_name} || "");
  $full_name =~ s&^\s*&&;
  $full_name =~ s&\s+& &g;
  $people_data->{$primary_email}->{full_name} = $full_name;
  my $contact_string = sprintf("%s <%s>",
                               $full_name,
                               $primary_email);
  $contact_string =~ s&^\s*&&;
  $people_data->{$primary_email}->{contact_string} = $contact_string;
}

sub by_lastname {
  return $people_data->{$a}->{last_name} cmp $people_data->{$b}->{last_name} or
    "$people_data->{$a}->{middle_name} $people_data->{$a}->{middle_name}" cmp
      "$people_data->{$b}->{first_name} $people_data->{$b}->{first_name}";
}

__END__

### Local Variables:
### mode: cperl
### End:
