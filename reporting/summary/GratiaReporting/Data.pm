package GratiaReporting::Data;

use strict;
require Exporter;

use XML::LibXML; # Parse XML file
use FileHandle;

use GratiaReporting::Person;

use vars qw(@ISA @EXPORT @EXPORT_OK $vo_data $site_data $people_data @test_addresses $default_data_sources);

@ISA = qw(Exporter);

@EXPORT = qw($vo_data $site_data $people_data @test_addresses);
@EXPORT_OK = qw($default_data_sources);

my $parser = XML::LibXML->new();

$default_data_sources =
  [
   'https://myosg.grid.iu.edu/vosummary/xml?datasource=summary&summary_attrs_showdesc=on&summary_attrs_showmember_resource=on&summary_attrs_showfield_of_science=on&summary_attrs_showreporting_group=on&summary_attrs_showparent_vo=on&all_vos=on&active=on&active_value=1',
   'https://myosg.grid.iu.edu/wizardsummary/xml?datasource=summary&summary_attrs_showdesc=on&summary_attrs_showservice=on&summary_attrs_showrsvstatus=on&summary_attrs_showgipstatus=on&summary_attrs_showfqdn=on&summary_attrs_showvomembership=on&summary_attrs_showvoownership=on&summary_attrs_showwlcg=on&summary_attrs_showenv=on&summary_attrs_showcontact=on&gip_status_attrs_showtestresults=on&gip_status_attrs_showfqdn=on&account_type=cumulative_hours&ce_account_type=gip_vo&se_account_type=vo_transfer_volume&all_resources=on&gridtype=on&gridtype_1=on&service=on&service_1=on&active=on&active_value=1&disable_value=1'
  ];

sub new {
  my $class = shift;
  my $self = { options => { @_ } };
  bless $self, $class;
  $self->initialize();
  $self->process_data();
  return $self;
}

sub initialize {
  my $self = shift;

  my $test_cert_info =
    {
     "cert-path" => glob("~/.globus/usercert.pem"),
     "key-path" => glob("~/.globus/userkey.pem"),
    };

  my $default_cert_info =
    {
     "cert-path" => "/etc/grid-security/gratia/gratiacert.pem",
     "key-path" => "/etc/grid-security/gratia/gratiakey.pem"
    };

  # Use Service Cert if we can find and read it; else personal cert.
  my $cert_info = ( -r $default_cert_info->{"key-path"} )?$default_cert_info:$test_cert_info;

  $self->{extra_wget_options} =
    [ "--certificate=$cert_info->{\"cert-path\"}",
      "--private-key=$cert_info->{\"key-path\"}",
      "--no-check-certificate", # Necessary because of use of http certificiate
      "--ca-directory=/etc/grid-security/certificates"
    ];

  # Set up data sources
  $self->{options}->{data_source} = [ ':default' ]
    unless ($self->{options}->{data_source});

  $self->{data_sources} = [];

  foreach my $data_source (@{$self->{options}->{data_source}}) {
    push @{$self->{data_sources}}, ($data_source eq ':default')?@$default_data_sources:$data_source;
  }

}

sub process_data {
  my $self = shift;
  foreach my $data_source (@{$self->{data_sources}}) {
    if ($self->{options}->{verbose}) {
      print STDERR "Reading from $data_source\n";
    }
    my $fh;
    if ($data_source =~ m&^(\w+)://&) { # Use wget to obtain XML.
      my $url_cmd = sprintf('wget %s -q -O - "%s" 2>/dev/null|',
                            join(" ", @{$self->{extra_wget_options} || []}),
                            $data_source);
      $self->verbosePrint("DEBUG: executing $url_cmd\n");
      $fh = FileHandle->new("$url_cmd") or
        die "Unable to open wget pipe for $data_source";
    } else {
      $fh = FileHandle->new("$data_source") or
        die "Unable to open \"$data_source\"";
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
  $self->verbosePrint("DEBUG: Reading OIM ", $root->nodeName , " data\n");
  if ($root->nodeName eq 'resource_contacts') {
    $self->verbosePrint("DEBUG: Calling processXmlResourceContacts()\n");
    $self->processXmlResourceContacts($root); # OIM site info
  } elsif ($root->nodeName eq 'ResourceSummary') {
    $self->verbosePrint("DEBUG: Calling processXmlResourceSummary()\n");
    $self->processXmlResourceSummary($root); # OIM site info (MyOSG)
  } elsif ($root->nodeName eq 'vo_contacts') {
    $self->verbosePrint("DEBUG: Calling processXmlVoContacts()\n");
    $self->processXmlVoContacts($root); # OIM VO info
  } elsif ($root->nodeName eq 'VOSummary') {
    $self->verbosePrint("DEBUG: Calling processXmlVOSummary()\n");
    $self->processXmlVOSummary($root); # OIM VO info (MyOSG)
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
      print "WARNING: info for $vo_name has already been seen: merging.\n";
    } else {
      $vo_data->{$vo_name} = {};
    }
    $this_vo = $vo_data->{$vo_name};
    $this_vo->{alt_vos} = [] unless $this_vo->{alt_vos};
    push @{$this_vo->{alt_vos}}, $vo_name;
    $this_vo->{science_fields} = {} unless $this_vo->{science_fields};
    my $science_nodes = $vo->findnodes('vo_fields_of_science/vo_field_of_science');
    foreach my $science_node ($science_nodes->get_nodelist()) {
      $this_vo->{science_fields}->{$science_node->string_value()} = 1;
    }
    my $reporting_group_nodes = $vo->findnodes('vo_reporting_group');
    $this_vo->{reporting_groups} = {} unless $this_vo->{reporting_groups};
    $this_vo->{reporting_contacts} = [] unless $this_vo->{reporting_contacts};
    foreach my $reporting_group_node ($reporting_group_nodes->get_nodelist()) {
      my $vo_reporting_name = $reporting_group_node->findvalue('vo_reporting_name');
      $this_vo->{reporting_groups}->{$vo_reporting_name} = {}
        unless $this_vo->{reporting_groups}->{$vo_reporting_name};
      my $this_reporting_group = $this_vo->{reporting_groups}->{$vo_reporting_name};
      my $reporting_contacts;
      # If the reporting name is the same as the VO name (almost) then
      # this is the primary contact list for this VO.
      if ($vo_name =~ m&^(?:us)?\Q$vo_reporting_name\E$&i or
          $vo_reporting_name =~ m&^(?:us)?\Q$vo_name\E$&i) {
        $reporting_contacts = $this_vo->{reporting_contacts};
      } else {
        $this_reporting_group->{reporting_contacts} = []
          unless $this_reporting_group->{reporting_contacts};
        $reporting_contacts = $this_reporting_group->{reporting_contacts};
      }
      $this_reporting_group->{FQAN} = [] unless $this_reporting_group->{FQAN};
      my $fqan_nodes = $reporting_group_node->findnodes('vo_fqan_group/fqan');
      my %fqan_node_set  = ( (map { $_?($_ => 1):(); } @{$this_reporting_group->{FQAN}}),
                             (map { $_?($_->string_value() => 1):() } $fqan_nodes->get_nodelist()) );
      $this_reporting_group->{FQAN} = [ sort keys %fqan_node_set ];
      my $reporting_contact_nodes = $reporting_group_node->findnodes('reporting_contact');
      foreach my $reporting_contact ($reporting_contact_nodes->get_nodelist()) {
        my $primary_email = $reporting_contact->findvalue('primary_email')
          || $reporting_contact->findvalue('alt_email'); # Contact's email
        next unless $primary_email;
        my $person = {};
        push @{$reporting_contacts}, $primary_email;
        $person->{vo_reporting_names} = [ ] unless $person->{vo_reporting_names};
        push @{$person->{vo_reporting_names}}, "$vo_name:$vo_reporting_name";
        foreach my $attribute qw(first_name middle_name last_name) { # Name info
          $person->{$attribute} =
            $reporting_contact->findvalue($attribute);
        }
        $self->mergePersonData($primary_email, $person);
      }
    }
  }
}

sub verbosePrint {
  my $self = shift;
  return unless $self->{options}->{verbose};
  print STDERR @_;
}

sub processXmlVOSummary {
  my ($self, $root) = @_;
  foreach my $vo ($root->findnodes('VO')) { # Each vo
    my $vo_name = $vo->findvalue('Name');
    my $long_name = $vo->findvalue('LongName');
    $self->verbosePrint("DEBUG: found VO $vo_name",
                        $long_name?(" (", $long_name, ")"):(),
                        "\n");
    my $parent_vo = $vo->findvalue('ParentVO/Name');
    if ($parent_vo) {
      if ($parent_vo eq $vo_name) {
        print "WARNING: VO $vo_name is listed as its own parent!\n";
      } else {
        print "INFO: Storing info for $vo_name ($long_name) as parent VO $parent_vo\n";
        $vo_name = $parent_vo;
      }
    } elsif ($long_name =~ m&^/?([^/]+)/& or $vo_name =~ m&^(fermilab).&i) {
      # Attempt to merge sub-VOs if parent VO info is missing.
      print "INFO: Deduced parent VO of $vo_name ($long_name) as $1\n";
      $vo_name = $1;
    }
    my $this_vo;
    $vo_data->{$vo_name} = {} unless exists $vo_data->{$vo_name};
    $this_vo = $vo_data->{$vo_name};
    $this_vo->{alt_vos} = [] unless $this_vo->{alt_vos};
    push @{$this_vo->{alt_vos}}, $vo_name;
    $this_vo->{science_fields} = {} unless $this_vo->{science_fields};
    my $science_nodes = $vo->findnodes('FieldsOfScience/Field');
    foreach my $science_node ($science_nodes->get_nodelist()) {
      $self->verbosePrint("DEBUG: found field of science ",
                          $science_node->string_value(),
                          "\n");
      $this_vo->{science_fields}->{$science_node->string_value()} = 1;
    }
    my $reporting_group_nodes = $vo->findnodes('ReportingGroups/ReportingGroup');
    $this_vo->{reporting_groups} = {} unless $this_vo->{reporting_groups};
    $this_vo->{reporting_contacts} = [] unless $this_vo->{reporting_contacts};
    foreach my $reporting_group_node ($reporting_group_nodes->get_nodelist()) {
      my $vo_reporting_name = $reporting_group_node->findvalue('Name');
      next unless $vo_reporting_name;
      $self->verbosePrint("DEBUG: found reporting group $vo_reporting_name\n");
      $this_vo->{reporting_groups}->{$vo_reporting_name} = {}
        unless $this_vo->{reporting_groups}->{$vo_reporting_name};
      my $this_reporting_group = $this_vo->{reporting_groups}->{$vo_reporting_name};
      my $reporting_contacts;
      # If the reporting name is the same as the VO name (almost) then
      # this is the primary contact list for this VO.
      if ($vo_name =~ m&^(?:us)?\Q$vo_reporting_name\E$&i or
          $vo_reporting_name =~ m&^(?:us)?\Q$vo_name\E$&i) {
        $reporting_contacts = $this_vo->{reporting_contacts};
      } else {
        $this_reporting_group->{reporting_contacts} = []
          unless $this_reporting_group->{reporting_contacts};
        $reporting_contacts = $this_reporting_group->{reporting_contacts};
      }
      $this_reporting_group->{FQAN} = [] unless $this_reporting_group->{FQAN};
      my $fqan_nodes = $reporting_group_node->findnodes('FQANs/FQAN');
      my %fqan_node_set  = ( (map { $_?($_ => 1):(); } @{$this_reporting_group->{FQAN}}),
                             (map { if ($_) {
                               my $result = $_->findvalue('GroupName') || '';
                               $result = sprintf("$result/Role=%s",
                                                 $_->findvalue('Role'))
                                 if $_->findvalue('Role');
                               $self->verbosePrint("DEBUG: found FQAN $result for reporting name $vo_reporting_name\n")
                                 if $result;
                               ($result => 1);
                             } else { () } } $fqan_nodes->get_nodelist()) );
      $this_reporting_group->{FQAN} = [ sort keys %fqan_node_set ];
      my $reporting_contact_nodes = $reporting_group_node->findnodes('Contacts/Contact');
      foreach my $reporting_contact ($reporting_contact_nodes->get_nodelist()) {
        my $primary_email = $reporting_contact->findvalue('Email');
        next unless $primary_email;
        my $name = $reporting_contact->findvalue('Name');
        $self->verbosePrint("DEBUG: found reporting contact ",
                            $name?"$name ":'',
                            "<",
                            $primary_email,
                            ">\n");
        my $person = {};
        push @{$reporting_contacts}, $primary_email;
        $person->{vo_reporting_names} = [ ] unless $person->{vo_reporting_names};
        push @{$person->{vo_reporting_names}}, "$vo_name:$vo_reporting_name";
        $person->{'last_name'} = $name;
        $self->mergePersonData($primary_email, $person);
      }
    }
  }
}

sub processXmlResourceSummary {
  my ($self, $root) = @_;
  foreach my $resource ($root->findnodes('ResourceGroup/Resources/Resource')) { # Each resource
    my $site_name = $resource->findvalue('Name'); # Resource's name
    $self->verbosePrint("DEBUG: found resource $site_name\n");
    my $this_site;
    if (exists $site_data->{$site_name}) {
      print STDERR "WARNING: info for $site_name has already been seen: merging.\n";
    } else {
      $site_data->{$site_name} = { name => $site_name };
    }
    $this_site = $site_data->{$site_name};
    $this_site->{grid_type} = $resource->findvalue('GridType'); # Grid type for resource
    my $service_nodes = $resource->findnodes('Services/Service'); # Services
    $this_site->{services} = {} unless $this_site->{services};
    foreach my $service_node ($service_nodes->get_nodelist()) {
      $this_site->{services}->{$service_node->findvalue('Name')} =
        {
         hidden => ($service_node->findvalue('HiddenService') =~ m&^(true|t|1)$&)?1:0
        };
    }
    $this_site->{owner_vos} = {} unless $this_site->{owner_vos};
    my $owner_nodes =  $resource->findnodes('VOOwnership/Ownership');
    foreach my $owner_node ($owner_nodes->get_nodelist()) {
      $this_site->{owner_vos}->{$owner_node->findvalue('VO')} =
        $owner_node->findvalue('Percent');
    }
    $this_site->{reporting_contacts} = [ ] unless $this_site->{reporting_contacts};
    # Find only reporting contacts, not any other kind of contact.
    my $reporting_contact_nodes = $resource->findnodes("ContactLists/ContactList[child::ContactType='Resource Report Contact']/Contacts/Contact");
    foreach my $reporting_contact ($reporting_contact_nodes->get_nodelist()) { # Each contact
      my $primary_email = $reporting_contact->findvalue('Email');
      next unless $primary_email;
      my $name = $reporting_contact->findvalue('Name');
      $self->verbosePrint("DEBUG: found reporting contact ",
                          $name?"$name ":'',
                          "<",
                          $primary_email,
                          ">\n");
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

sub processXmlResourceContacts {
  my ($self, $root) = @_;
  foreach my $resource ($root->findnodes('resource')) { # Each resource
    my $site_name = $resource->findvalue('resource_name'); # Resource's name
    my $this_site;
    if (exists $site_data->{$site_name}) {
      print STDERR "WARNING: info for $site_name has already been seen: merging.\n";
    } else {
      $site_data->{$site_name} = { name => $site_name };
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
    $vo_data->{$vo_name} = {} unless exists $vo_data->{$vo_name};
    if (exists $vo_data->{$vo_name}->{alt_vos} and exists $vo->{alt_vos}) {
      push @{$vo_data->{$vo_name}->{alt_vos}}, @{$vo->{alt_vos}};
    } else {
      $vo_data->{$vo_name}->{alt_vos} = $vo->{alt_vos} || [];
      push @{$vo_data->{$vo_name}->{alt_vos}}, $vo_name; # Case correction of real VO name.
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
  my ($self, $person_email, $person_hash) = @_;
  if (exists $people_data->{$person_email}) {
    $people_data->{$person_email}->mergePersonData($person_hash);
  } else {
    $person_hash->{primary_email} = $person_email unless $person_hash->{primary_email};
    $people_data->{$person_email} = new GratiaReporting::Person($person_hash);
  }
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
